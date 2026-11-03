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
     * Resolve the public base of the application.
     *
     * @param context the current request
     * @return the base with no trailing slash, for example {@code https://ex.com} or {@code https://ex.com/app}
     */
    String resolve(RoutingContext context);
}
