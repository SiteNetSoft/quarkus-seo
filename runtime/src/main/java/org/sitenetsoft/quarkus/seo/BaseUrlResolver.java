package org.sitenetsoft.quarkus.seo;

import io.vertx.ext.web.RoutingContext;

/**
 * Resolve the public base URL of the application for a request.
 *
 * <p>
 * An application that knows its public address better than the request does provides its own bean of this type.
 */
public interface BaseUrlResolver {

    /**
     * Resolve the public URL of the application root.
     *
     * <p>
     * The extension appends paths relative to the root path to this URL. The URL therefore holds the public path
     * prefix of the application. The default bean returns {@code quarkus.seo.site-url} as it is when that key is
     * set. Otherwise it returns the request scheme and host followed by {@code quarkus.http.root-path}.
     *
     * @param context the current request
     * @return the URL with no trailing slash, for example {@code https://ex.com} or {@code https://ex.com/app}
     */
    String resolve(RoutingContext context);
}
