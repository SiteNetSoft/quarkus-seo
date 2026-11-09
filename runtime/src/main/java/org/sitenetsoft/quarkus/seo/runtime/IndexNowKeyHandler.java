package org.sitenetsoft.quarkus.seo.runtime;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Serve the IndexNow key file.
 */
final class IndexNowKeyHandler implements Handler<RoutingContext> {

    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    private final String key;

    /**
     * Create the handler.
     *
     * @param key the IndexNow key
     */
    IndexNowKeyHandler(String key) {
        this.key = key;
    }

    /**
     * Write the key as the whole body.
     *
     * @param context the routing context
     */
    @Override
    public void handle(RoutingContext context) {
        SeoConfig config = Arc.container().instance(SeoConfig.class).get();
        HttpOutputs.send(context, key, CONTENT_TYPE, config.sitemap().cacheControl());
    }
}
