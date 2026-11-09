package org.sitenetsoft.quarkus.seo.runtime;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Serve the web manifest.
 */
final class ManifestHandler implements Handler<RoutingContext> {

    private static final String CONTENT_TYPE = "application/manifest+json";

    /**
     * Write the manifest from the configuration.
     *
     * @param context the routing context
     */
    @Override
    public void handle(RoutingContext context) {
        SeoConfig config = Arc.container().instance(SeoConfig.class).get();
        SeoSettings settings = Arc.container().instance(SeoSettings.class).get();
        String body = ManifestWriter.write(config.manifest(), settings.siteName(), config.themeColor(),
                settings.rootPath());
        HttpOutputs.send(context, body, CONTENT_TYPE, config.sitemap().cacheControl());
    }
}
