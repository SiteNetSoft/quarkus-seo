package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Record build-time findings for the run time.
 */
@Recorder
public class SeoRecorder {

    /**
     * Create the supplier of the Quarkus locale keys.
     *
     * @param locales the tags of {@code quarkus.locales}, empty when the application did not set the key
     * @param defaultLocale the tag of {@code quarkus.default-locale}, empty when the application did not set the key
     * @return the supplier of the holder
     */
    public Supplier<QuarkusLocaleKeys> localeKeys(List<String> locales, String defaultLocale) {
        QuarkusLocaleKeys keys = new QuarkusLocaleKeys(locales, defaultLocale);
        return () -> keys;
    }

    /**
     * Create the filter that writes the {@code X-Robots-Tag} header.
     *
     * <p>
     * The filter reads the directives in the headers-end callback. It does not buffer the response body.
     *
     * @return the filter handler
     */
    public Handler<RoutingContext> robotsHeaderFilter() {
        return new RobotsHeaderFilter();
    }

    /**
     * Create the supplier of the sitemap entries found at build time.
     *
     * @param paths the paths of the entries
     * @param changefreqs the lower case change frequencies, with an empty string when unset
     * @param priorities the priorities, with -1 when unset
     * @return the supplier of the holder
     */
    public Supplier<SitemapEntries> sitemapEntries(List<String> paths, List<String> changefreqs,
            List<Double> priorities) {
        SitemapEntries entries = new SitemapEntries(paths, changefreqs, priorities);
        return () -> entries;
    }

    /**
     * Create the handler of the sitemap path.
     *
     * @return the handler that serves the sitemap or the sitemap index
     */
    public Handler<RoutingContext> sitemapHandler() {
        return new SitemapHandler(false);
    }

    /**
     * Create the handler of the sitemap page path.
     *
     * @return the handler that reads the page number from the first regex group
     */
    public Handler<RoutingContext> sitemapPageHandler() {
        return new SitemapHandler(true);
    }

    /**
     * Create the handler of robots.txt.
     *
     * @return the handler that serves the file
     */
    public Handler<RoutingContext> robotsHandler() {
        return new RobotsHandler();
    }

    /**
     * Create the handler of llms.txt.
     *
     * @return the handler that serves the file
     */
    public Handler<RoutingContext> llmsTxtHandler() {
        return new LlmsTxtHandler();
    }

    /**
     * Create the handler of security.txt.
     *
     * @param underRootPath true when the route sits under the HTTP root path
     * @return the handler that serves the file
     */
    public Handler<RoutingContext> securityTxtHandler(boolean underRootPath) {
        return new SecurityTxtHandler(underRootPath);
    }

    /**
     * Create the handler of the web manifest.
     *
     * @return the handler that serves the manifest
     */
    public Handler<RoutingContext> manifestHandler() {
        return new ManifestHandler();
    }

    /**
     * Create the supplier of the manifest link holder.
     *
     * @param href the href of the link, or an empty string when the extension serves no manifest
     * @return the supplier of the holder
     */
    public Supplier<ManifestLink> manifestLink(String href) {
        ManifestLink link = new ManifestLink(href.isEmpty() ? Optional.empty() : Optional.of(href));
        return () -> link;
    }

    /**
     * Create the handler of the IndexNow key file.
     *
     * @param key the IndexNow key
     * @return the handler that serves the key
     */
    public Handler<RoutingContext> indexNowKeyHandler(String key) {
        return new IndexNowKeyHandler(key);
    }

    /**
     * Create the supplier of the IndexNow key holder.
     *
     * @param key the IndexNow key
     * @param underRootPath true when the key file is served under the HTTP root path
     * @return the supplier of the holder
     */
    public Supplier<IndexNowKey> indexNowKey(String key, boolean underRootPath) {
        IndexNowKey holder = new IndexNowKey(key, underRootPath);
        return () -> holder;
    }

    /**
     * Create a route function that matches GET and HEAD requests on a path.
     *
     * @param path the exact path
     * @return the route function
     */
    public Function<Router, Route> getRoute(String path) {
        return router -> router.route(path).method(HttpMethod.GET).method(HttpMethod.HEAD);
    }

    /**
     * Create a route customizer that limits a route to GET and HEAD requests.
     *
     * @return the route customizer
     */
    public Consumer<Route> getAndHead() {
        return route -> route.method(HttpMethod.GET).method(HttpMethod.HEAD);
    }

    /**
     * Create a route function that matches GET and HEAD requests on a regular expression.
     *
     * @param regex the regular expression of the path
     * @return the route function
     */
    public Function<Router, Route> getRegexRoute(String regex) {
        return router -> router.routeWithRegex(regex).method(HttpMethod.GET).method(HttpMethod.HEAD);
    }

    /**
     * Log once that a cache key is on while {@code quarkus-cache} is absent.
     *
     * @param sitemap true when the extension serves the sitemap
     * @param robots true when the extension serves robots.txt
     */
    public void logCacheMissing(boolean sitemap, boolean robots) {
        Config config = ConfigProvider.getConfig();
        boolean sitemapCache = sitemap && config.getOptionalValue("quarkus.seo.sitemap.cache.enabled", Boolean.class)
                .orElse(false);
        boolean robotsCache = robots && config.getOptionalValue("quarkus.seo.robots.cache.enabled", Boolean.class)
                .orElse(false);
        if (sitemapCache || robotsCache) {
            Logger.getLogger(SeoRecorder.class)
                    .info("quarkus.seo cache is enabled but quarkus-cache is not present. Serving without a cache.");
        }
    }
}
