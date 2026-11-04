package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Write the {@code X-Robots-Tag} header when the response headers leave.
 */
final class RobotsHeaderFilter implements Handler<RoutingContext> {

    private static final String HEADER = "X-Robots-Tag";
    private static final String NOT_INDEXABLE = "noindex, nofollow";

    private volatile SeoSettings settings;

    /**
     * Register the headers-end callback and pass the request on.
     *
     * @param context the routing context
     */
    @Override
    public void handle(RoutingContext context) {
        context.addHeadersEndHandler(done -> write(context));
        context.next();
    }

    private void write(RoutingContext context) {
        if (context.response().headers().contains(HEADER)) {
            return;
        }
        SeoSettings current = settings();
        if (current != null && !current.indexable()) {
            context.response().headers().set(HEADER, NOT_INDEXABLE);
            return;
        }
        List<String> directives = RobotsHeader.get(context);
        if (!directives.isEmpty()) {
            context.response().headers().set(HEADER, String.join(", ", directives));
        }
    }

    private SeoSettings settings() {
        SeoSettings current = settings;
        if (current == null) {
            ArcContainer container = Arc.container();
            if (container == null) {
                return null;
            }
            InstanceHandle<SeoSettings> handle = container.instance(SeoSettings.class);
            if (!handle.isAvailable()) {
                return null;
            }
            current = handle.get();
            settings = current;
        }
        return current;
    }
}
