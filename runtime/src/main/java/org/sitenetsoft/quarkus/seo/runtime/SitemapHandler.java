package org.sitenetsoft.quarkus.seo.runtime;

import java.util.Optional;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;

/**
 * Serve the sitemap, the sitemap index or one sitemap page on a worker thread.
 */
final class SitemapHandler implements Handler<RoutingContext> {

    private static final String CONTENT_TYPE = "application/xml; charset=utf-8";
    private static final String PAGE_PARAM = "param0";

    private final boolean pageRoute;

    /**
     * Create the handler.
     *
     * @param pageRoute true when the handler reads the page number from the first regex group
     */
    SitemapHandler(boolean pageRoute) {
        this.pageRoute = pageRoute;
    }

    /**
     * Write the requested sitemap document, or answer 404 when the page does not exist.
     *
     * @param context the routing context
     */
    @Override
    public void handle(RoutingContext context) {
        int page = pageRoute ? pageNumber(context.pathParam(PAGE_PARAM)) : 0;
        if (page < 0) {
            context.response().setStatusCode(404).end();
            return;
        }
        SitemapService service = Arc.container().instance(SitemapService.class).get();
        String baseUrl = Arc.container().instance(BaseUrlResolver.class).get().resolve(context);
        Optional<String> xml;
        try {
            xml = service.page(baseUrl, page);
        } catch (SitemapService.ContributorException e) {
            context.response().setStatusCode(500).end();
            return;
        }
        if (xml.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }
        String cacheControl = Arc.container().instance(SeoConfig.class).get().sitemap().cacheControl();
        HttpOutputs.send(context, xml.get(), CONTENT_TYPE, cacheControl);
    }

    private static int pageNumber(String value) {
        if (value == null) {
            return -1;
        }
        try {
            int page = Integer.parseInt(value);
            return page >= 1 ? page : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
