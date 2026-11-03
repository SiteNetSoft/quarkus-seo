package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * Create a route function that matches GET and HEAD requests on a path.
     *
     * @param path the exact path
     * @return the route function
     */
    public Function<Router, Route> getRoute(String path) {
        return router -> router.route(path).method(HttpMethod.GET).method(HttpMethod.HEAD);
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
}
