package org.sitenetsoft.quarkus.seo.runtime;

import java.util.Optional;

import io.quarkus.arc.Arc;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;

/**
 * Serve security.txt on a worker thread.
 */
final class SecurityTxtHandler implements Handler<RoutingContext> {
    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    private final boolean underRootPath;

    /**
     * Create the handler.
     *
     * @param underRootPath true when the route sits under the HTTP root path
     */
    SecurityTxtHandler(boolean underRootPath) {
        this.underRootPath = underRootPath;
    }

    /**
     * Write the file from the configuration, or answer 404 when no contact is set.
     *
     * @param context the routing context
     */
    @Override
    public void handle(RoutingContext context) {
        SeoConfig config = Arc.container().instance(SeoConfig.class).get();
        String baseUrl = Arc.container().instance(BaseUrlResolver.class).get().resolve(context);
        String root = underRootPath ? Urls.absolute(baseUrl, "") : Urls.hostRoot(baseUrl);
        Optional<String> body = SecurityTxtWriter.write(config.securityTxt(), root + SeoPaths.SECURITY_TXT);
        if (body.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }
        HttpOutputs.send(context, body.get(), CONTENT_TYPE, config.sitemap().cacheControl());
    }
}
