package org.sitenetsoft.quarkus.seo.runtime.devui;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.SeoHeadRenderer;
import org.sitenetsoft.quarkus.seo.SeoLocales;
import org.sitenetsoft.quarkus.seo.SitemapUrl;
import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoChecks;
import org.sitenetsoft.quarkus.seo.runtime.SeoConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;
import org.sitenetsoft.quarkus.seo.runtime.SeoWarning;
import org.sitenetsoft.quarkus.seo.runtime.SitemapService;

/**
 * Answer the JSON-RPC calls of the SEO Dev UI pages.
 *
 * <p>
 * The service exists in dev mode only. It fetches pages of the running application over HTTP and reads the heads that
 * {@link SeoDevCapture} records. It never fetches another host. Every method returns a map, and a failure shows as an
 * {@code error} entry instead of an exception.
 */
@Singleton
public class SeoJsonRpcService {

    private static final int MAX_HREFLANG_FETCHES = 20;
    private static final int MAX_SITEMAP_FETCHES = 200;
    private static final int DEFAULT_PORT = 8080;
    private static final String ERROR = "error";
    private static final String WARNINGS = "warnings";

    private final SeoConfig config;
    private final SeoBuildConfig buildConfig;
    private final SeoSettings settings;
    private final SeoLocales locales;
    private final Instance<SeoDevCapture> capture;
    private final Instance<SeoHeadRenderer> renderer;
    private final Instance<SitemapService> sitemap;
    private final PageFetcher fetcher;

    /**
     * Create the service.
     *
     * @param config the run-time SEO configuration
     * @param buildConfig the build-time SEO configuration
     * @param settings the resolved site settings
     * @param locales the site locales
     * @param capture the dev capture, which exists when the meta tag feature is on
     * @param renderer the head renderer, which exists when the meta tag feature is on
     * @param sitemap the sitemap service, which exists when the extension serves the sitemap or llms.txt
     */
    @Inject
    public SeoJsonRpcService(SeoConfig config, SeoBuildConfig buildConfig, SeoSettings settings, SeoLocales locales,
            Instance<SeoDevCapture> capture, Instance<SeoHeadRenderer> renderer, Instance<SitemapService> sitemap) {
        this.config = config;
        this.buildConfig = buildConfig;
        this.settings = settings;
        this.locales = locales;
        this.capture = capture;
        this.renderer = renderer;
        this.sitemap = sitemap;
        this.fetcher = new PageFetcher(settings.rootPath());
    }

    /**
     * Describe the site settings and the security.txt problems.
     *
     * @return the site URL, the site name, the locales, the default locale, the indexable flag and the security.txt
     *         warnings
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("siteUrl", configuredSiteUrl().orElse("request host"));
        result.put("siteName", settings.siteName().orElse(""));
        List<String> tags = new ArrayList<>();
        for (Locale locale : locales.locales()) {
            tags.add(locale.toLanguageTag());
        }
        result.put("locales", tags);
        result.put("defaultLocale", locales.defaultLocale().toLanguageTag());
        result.put("indexable", settings.indexable());
        List<SeoWarning> securityTxt = List.of();
        if (buildConfig.enabled() && buildConfig.securityTxt().enabled()) {
            securityTxt = SeoChecks.securityTxt(config.securityTxt(), Instant.now());
        }
        result.put("securityTxtWarnings", warnings(securityTxt));
        return result;
    }

    /**
     * Fetch one page and check its head.
     *
     * <p>
     * The method also fetches up to 20 alternate language pages on the same site to find one-way hreflang links.
     *
     * @param path the page path relative to the HTTP root path, as typed in the Dev UI
     * @return the status, the rendered head, the search snippet, the social card and the warnings, or an
     *         {@code error} entry when the path is not local or the fetch fails
     */
    public Map<String, Object> checkPage(String path) {
        Optional<String> local = PageFetcher.localPath(path);
        if (local.isEmpty()) {
            return error("Enter a path that starts with a single slash, such as /about.");
        }
        if (!capture.isResolvable() || !renderer.isResolvable()) {
            return error("The meta tag feature is switched off.");
        }
        Fetched page;
        try {
            page = fetch(local.get());
        } catch (IOException e) {
            return error("The request to " + local.get() + " failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("The request to " + local.get() + " was interrupted.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", page.status());
        if (page.seo().isEmpty()) {
            result.put("head", "");
            result.put(WARNINGS, warnings(List.of(noHead(page.status()))));
            return result;
        }
        ResolvedSeo seo = page.seo().get();
        String pageUrl = seo.canonical().orElse(siteBase() + PageFetcher.capturePath(local.get()));
        List<SeoWarning> found = new ArrayList<>(SeoChecks.page(seo));
        found.addAll(SeoChecks.oneWayHreflang(pageUrl, seo.hreflang(), alternates(seo, local.get())));
        result.put("head", renderer.get().render(seo));
        result.put("snippet", snippet(seo, pageUrl));
        result.put("card", card(seo));
        result.put(WARNINGS, warnings(found));
        return result;
    }

    /**
     * Fetch the sitemap pages on the site and check their heads.
     *
     * <p>
     * The method fetches at most 200 pages. It counts the other pages and the pages on other hosts as skipped.
     *
     * @return the checked count, the skipped count and the warnings, or an {@code error} entry when the sitemap
     *         sources are unavailable
     */
    public Map<String, Object> checkSitemap() {
        if (!sitemap.isResolvable()) {
            return error("The extension serves neither the sitemap nor llms.txt.");
        }
        if (!capture.isResolvable()) {
            return error("The meta tag feature is switched off.");
        }
        List<SitemapUrl> urls;
        try {
            urls = sitemap.get().urls();
        } catch (SitemapService.ContributorException e) {
            return error(e.getMessage());
        }
        String base = siteBase();
        Set<String> paths = new LinkedHashSet<>();
        int skipped = 0;
        for (SitemapUrl url : urls) {
            Optional<String> local = localPathOf(url.loc(), base);
            if (local.isEmpty()) {
                skipped++;
            } else if (!paths.contains(local.get())) {
                if (paths.size() < MAX_SITEMAP_FETCHES) {
                    paths.add(local.get());
                } else {
                    skipped++;
                }
            }
        }
        Map<String, String> titleByPath = new LinkedHashMap<>();
        List<Map<String, Object>> pageEntries = new ArrayList<>();
        for (String path : paths) {
            List<SeoWarning> found;
            Integer status = null;
            try {
                Fetched page = fetch(path);
                status = page.status();
                if (page.seo().isEmpty()) {
                    found = List.of(noHead(page.status()));
                } else {
                    titleByPath.put(path, page.seo().get().title());
                    found = SeoChecks.page(page.seo().get());
                }
            } catch (IOException e) {
                found = List.of(new SeoWarning("fetch-failed", "The request failed: " + e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return error("The sitemap check was interrupted.");
            }
            if (!found.isEmpty()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("url", path);
                if (status != null) {
                    entry.put("status", status);
                }
                entry.put(WARNINGS, warnings(found));
                pageEntries.add(entry);
            }
        }
        List<Map<String, Object>> entries = new ArrayList<>(warnings(SeoChecks.duplicateTitles(titleByPath)));
        entries.addAll(pageEntries);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", paths.size());
        result.put("skipped", skipped);
        result.put(WARNINGS, entries);
        return result;
    }

    private Fetched fetch(String path) throws IOException, InterruptedException {
        String key = PageFetcher.capturePath(path);
        SeoDevCapture devCapture = capture.get();
        devCapture.clear(key);
        int status = fetcher.fetch(path);
        return new Fetched(status, devCapture.last(key));
    }

    private Map<String, List<ResolvedSeo.Link>> alternates(ResolvedSeo seo, String path) {
        Map<String, List<ResolvedSeo.Link>> result = new LinkedHashMap<>();
        String base = siteBase();
        String own = PageFetcher.capturePath(path);
        int fetches = 0;
        for (ResolvedSeo.Link link : seo.hreflang()) {
            if (fetches >= MAX_HREFLANG_FETCHES) {
                break;
            }
            if ("x-default".equals(link.hreflang()) || result.containsKey(link.href())) {
                continue;
            }
            Optional<String> local = localPathOf(link.href(), base);
            if (local.isEmpty() || PageFetcher.capturePath(local.get()).equals(own)) {
                continue;
            }
            fetches++;
            try {
                Fetched page = fetch(local.get());
                if (page.seo().isPresent()) {
                    result.put(link.href(), page.seo().get().hreflang());
                }
            } catch (IOException e) {
                // An alternate page that does not answer gives no return links to compare.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return result;
    }

    private static Optional<String> localPathOf(String loc, String base) {
        if (loc == null || loc.isBlank()) {
            return Optional.empty();
        }
        String value = loc.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || value.startsWith("//")) {
            if (lower.equals(base.toLowerCase(Locale.ROOT))) {
                return Optional.of("/");
            }
            String prefix = base + "/";
            if (!lower.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return Optional.empty();
            }
            return PageFetcher.localPath(value.substring(base.length()));
        }
        int start = 0;
        while (start < value.length() && value.charAt(start) == '/') {
            start++;
        }
        return PageFetcher.localPath("/" + value.substring(start));
    }

    private Optional<String> configuredSiteUrl() {
        return config.siteUrl().map(String::trim).filter(value -> !value.isEmpty());
    }

    private String siteBase() {
        Optional<String> siteUrl = configuredSiteUrl();
        if (siteUrl.isPresent()) {
            return stripTrailingSlashes(siteUrl.get());
        }
        int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.port", Integer.class).orElse(DEFAULT_PORT);
        return "http://localhost:" + port + settings.rootPath();
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static Map<String, Object> snippet(ResolvedSeo seo, String pageUrl) {
        Map<String, Object> snippet = new LinkedHashMap<>();
        snippet.put("title", seo.title() == null ? "" : seo.title());
        snippet.put("url", pageUrl);
        snippet.put("description", seo.description().orElse(""));
        return snippet;
    }

    private static Map<String, Object> card(ResolvedSeo seo) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("title", seo.ogTitle().orElse(seo.title() == null ? "" : seo.title()));
        card.put("description", seo.description().orElse(""));
        card.put("image", seo.image().isPresent() ? seo.image().get().url() : "");
        card.put("siteName", seo.siteName().orElse(""));
        return card;
    }

    private static SeoWarning noHead(int status) {
        return new SeoWarning("no-seo-head",
                "No SEO head found. The page answered " + status + " and rendered no {#seo} tag.");
    }

    private static List<Map<String, Object>> warnings(List<SeoWarning> warnings) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SeoWarning warning : warnings) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", warning.code());
            entry.put("message", warning.message());
            result.add(entry);
        }
        return result;
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(ERROR, message);
        return result;
    }

    /**
     * Hold the result of one page fetch.
     *
     * @param status the HTTP status
     * @param seo the head that the capture recorded, or empty when the page rendered none
     */
    private record Fetched(int status, Optional<ResolvedSeo> seo) {
    }
}
