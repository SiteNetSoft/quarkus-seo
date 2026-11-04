package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;

import io.vertx.ext.web.RoutingContext;

/**
 * Carry the robots directives set in Java to the response filter.
 *
 * <p>
 * This class is internal. Application code does not use it.
 */
public final class RobotsHeader {

    /** The routing context key that holds the directives. */
    public static final String KEY = "org.sitenetsoft.seo.robots";

    private RobotsHeader() {
    }

    /**
     * Store the directives on the request.
     *
     * @param context the routing context
     * @param directives the directives set in Java
     */
    public static void put(RoutingContext context, List<String> directives) {
        context.put(KEY, List.copyOf(directives));
    }

    /**
     * Read the directives from the request.
     *
     * @param context the routing context
     * @return the directives, or an empty list when none are stored
     */
    public static List<String> get(RoutingContext context) {
        List<String> stored = context.get(KEY);
        return stored == null ? List.of() : stored;
    }
}
