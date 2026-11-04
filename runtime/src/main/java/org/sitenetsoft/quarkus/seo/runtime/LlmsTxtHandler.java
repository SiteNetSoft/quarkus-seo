package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

/**
 * Serve llms.txt on a worker thread.
 */
final class LlmsTxtHandler implements Handler<RoutingContext> {

    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    /**
     * Write the file from the sitemap sources, or answer 500 when a contributor fails.
     *
     * @param context the routing context
     */
    @Override
    public void handle(RoutingContext context) {
        SeoConfig config = Arc.container().instance(SeoConfig.class).get();
        SeoSettings settings = Arc.container().instance(SeoSettings.class).get();
        String baseUrl = Arc.container().instance(BaseUrlResolver.class).get().resolve(context);
        List<SitemapUrl> urls;
        try {
            urls = Arc.container().instance(SitemapService.class).get().urls();
        } catch (SitemapService.ContributorException e) {
            context.response().setStatusCode(500).end();
            return;
        }
        String body = LlmsTxtWriter.write(settings.siteName().filter(name -> !name.isBlank()).orElse(baseUrl),
                config.description(), urls, baseUrl);
        HttpOutputs.send(context, body, CONTENT_TYPE, config.sitemap().cacheControl());
    }
}
