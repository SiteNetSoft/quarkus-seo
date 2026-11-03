package org.sitenetsoft.quarkus.seo.runtime;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;
import org.sitenetsoft.quarkus.seo.RobotsTxtProvider;

/**
 * Serve robots.txt on a worker thread.
 */
final class RobotsHandler implements Handler<RoutingContext> {

    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    /**
     * Write the file that the robots provider returns.
     *
     * @param context the routing context
     */
    @Override
    public void handle(RoutingContext context) {
        String baseUrl = Arc.container().instance(BaseUrlResolver.class).get().resolve(context);
        String body = Arc.container().instance(RobotsTxtProvider.class).get().robotsTxt(baseUrl);
        String cacheControl = Arc.container().instance(SeoConfig.class).get().sitemap().cacheControl();
        HttpOutputs.send(context, body, CONTENT_TYPE, cacheControl);
    }
}
