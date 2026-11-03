package org.sitenetsoft.quarkus.seo.runtime;

import java.util.Optional;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;

/**
 * Describe the current request.
 *
 * @param baseUrl the public base URL with no trailing slash
 * @param path the request path relative to the root path, starting with a slash
 * @param rawQuery the raw query string without the question mark, or null
 * @param home true when the path is the home page
 */
public record RequestInfo(String baseUrl, String path, String rawQuery, boolean home) {

    /**
     * Read the current request.
     *
     * @param resolver the base URL resolver
     * @param settings the site settings
     * @return the request, or empty when no request is active
     */
    public static Optional<RequestInfo> current(BaseUrlResolver resolver, SeoSettings settings) {
        ArcContainer container = Arc.container();
        if (container == null || !container.requestContext().isActive()) {
            return Optional.empty();
        }
        InstanceHandle<CurrentVertxRequest> handle = container.instance(CurrentVertxRequest.class);
        RoutingContext context = handle.isAvailable() ? handle.get().getCurrent() : null;
        if (context == null) {
            return Optional.empty();
        }
        String full = context.request().path();
        return Optional.of(of(resolver.resolve(context), full, context.request().query(), settings.rootPath()));
    }

    /**
     * Build the description from request parts.
     *
     * @param baseUrl the public base URL
     * @param fullPath the full request path
     * @param rawQuery the raw query string, or null
     * @param rootPath the root path with no trailing slash
     * @return the description
     */
    static RequestInfo of(String baseUrl, String fullPath, String rawQuery, String rootPath) {
        String path = fullPath == null ? "" : fullPath;
        if (!rootPath.isEmpty() && (path.equals(rootPath) || path.startsWith(rootPath + "/"))) {
            path = path.substring(rootPath.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }
        return new RequestInfo(baseUrl, path, rawQuery, path.equals("/"));
    }
}
