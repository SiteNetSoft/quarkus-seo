package org.sitenetsoft.quarkus.seo.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.All;
import io.quarkus.arc.InstanceHandle;
import org.sitenetsoft.quarkus.seo.SeoLocales;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;
import org.sitenetsoft.quarkus.seo.SitemapWriter;

/**
 * Collect the sitemap URLs from every source and write the requested sitemap document.
 *
 * <p>
 * The service reads contributor streams, so callers run it on a worker thread.
 */
@Singleton
public class SitemapService {

    private static final Logger LOG = Logger.getLogger(SitemapService.class);
    private static final String PAGE_TOKEN = "{n}";

    private final SeoConfig config;
    private final SeoBuildConfig buildConfig;
    private final SitemapWriter writer;
    private final SeoLocales locales;
    private final SitemapEntries entries;
    private final List<InstanceHandle<SitemapContributor>> contributors;
    private final SeoOutputCache cache;

    /**
     * Create the service.
     *
     * @param config the run-time SEO configuration
     * @param buildConfig the build-time SEO configuration
     * @param writer the sitemap writer
     * @param locales the site locales
     * @param entries the entries found at build time
     * @param contributors the contributor beans, highest priority first
     * @param cache the output cache
     */
    @Inject
    public SitemapService(SeoConfig config, SeoBuildConfig buildConfig, SitemapWriter writer, SeoLocales locales,
            SitemapEntries entries, @All List<InstanceHandle<SitemapContributor>> contributors, SeoOutputCache cache) {
        this.config = config;
        this.buildConfig = buildConfig;
        this.writer = writer;
        this.locales = locales;
        this.entries = entries;
        this.contributors = contributors;
        this.cache = cache;
    }

    /**
     * Write one sitemap document.
     *
     * <p>
     * Page 0 is the whole sitemap. It is a URL set when the URL count is at most {@code max-urls}, and an index
     * otherwise. Page 1 and above are slices of {@code max-urls} URLs.
     *
     * <p>
     * The cache key is the base URL and the page number. A page that does not exist is never cached.
     *
     * @param baseUrl the base URL that makes paths absolute
     * @param page the page number
     * @return the XML, or empty when the page does not exist
     * @throws ContributorException when a contributor fails, after the failure is logged
     */
    public Optional<String> page(String baseUrl, int page) {
        if (page < 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(cache.get(SeoOutputCache.SITEMAP, baseUrl + "|" + page,
                    () -> generate(baseUrl, page).orElseThrow(MissingPage::new)));
        } catch (MissingPage e) {
            return Optional.empty();
        }
    }

    private Optional<String> generate(String baseUrl, int page) {
        List<SitemapUrl> urls = urls();
        int max = Math.max(1, config.sitemap().maxUrls());
        int pages = (urls.size() + max - 1) / max;
        if (page == 0) {
            if (urls.size() <= max) {
                return Optional.of(writer.urlSet(urls, baseUrl, locales.defaultLocale()));
            }
            List<String> pageUrls = new ArrayList<>(pages);
            for (int n = 1; n <= pages; n++) {
                pageUrls.add(Urls.absolute(baseUrl, SeoPaths.normalize(buildConfig.sitemap().pagePath())
                        .replace(PAGE_TOKEN, Integer.toString(n))));
            }
            return Optional.of(writer.index(pageUrls));
        }
        if (page > pages) {
            return Optional.empty();
        }
        int from = (page - 1) * max;
        int to = Math.min(urls.size(), from + max);
        return Optional.of(writer.urlSet(urls.subList(from, to), baseUrl, locales.defaultLocale()));
    }

    /**
     * Collect the URLs of every source, in order: the configured URLs, the {@code @SitemapEntry} URLs and the
     * contributor URLs.
     *
     * @return the URLs, which the caller may change
     * @throws ContributorException when a contributor fails, after the failure is logged
     */
    public List<SitemapUrl> urls() {
        List<SitemapUrl> urls = new ArrayList<>();
        for (String path : config.sitemap().urls().orElse(List.of())) {
            if (path != null && !path.isBlank()) {
                urls.add(SitemapUrl.of(path.trim()));
            }
        }
        urls.addAll(entries.urls());
        for (InstanceHandle<SitemapContributor> handle : contributors) {
            read(handle, urls);
        }
        return urls;
    }

    private static void read(InstanceHandle<SitemapContributor> handle, List<SitemapUrl> urls) {
        try (Stream<SitemapUrl> stream = handle.get().urls()) {
            if (stream == null) {
                return;
            }
            Iterator<SitemapUrl> iterator = stream.iterator();
            while (iterator.hasNext()) {
                SitemapUrl url = iterator.next();
                if (url != null && url.loc() != null && !url.loc().isBlank()) {
                    urls.add(url);
                }
            }
        } catch (RuntimeException e) {
            String name = handle.getBean().getBeanClass().getName();
            LOG.error("Sitemap contributor " + name + " failed", e);
            throw new ContributorException(name, e);
        }
    }

    /**
     * Signal inside the cache computation that the page does not exist, so the cache stores nothing.
     */
    private static final class MissingPage extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * Create the exception without a stack trace.
         */
        MissingPage() {
            super(null, null, false, false);
        }
    }

    /**
     * Signal that a sitemap contributor failed and that the failure is already logged.
     */
    public static final class ContributorException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * Create the exception.
         *
         * @param contributor the class name of the failing contributor
         * @param cause the failure
         */
        ContributorException(String contributor, Throwable cause) {
            super("Sitemap contributor " + contributor + " failed", cause);
        }
    }
}
