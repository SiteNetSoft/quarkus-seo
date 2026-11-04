package org.sitenetsoft.quarkus.seo;

import java.util.ArrayList;
import java.util.List;

/**
 * Hold the rule groups and the sitemap URLs that make up robots.txt.
 */
public final class RobotsRules {

    private final List<RobotsGroup> groups = new ArrayList<>();
    private final List<String> sitemaps = new ArrayList<>();

    /**
     * Return the rule groups.
     *
     * @return the mutable list of groups, in output order
     */
    public List<RobotsGroup> groups() {
        return groups;
    }

    /**
     * Return the sitemap URLs.
     *
     * @return the mutable list of absolute URLs
     */
    public List<String> sitemaps() {
        return sitemaps;
    }
}
