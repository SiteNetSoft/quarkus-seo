package org.sitenetsoft.quarkus.seo.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Carry one sitemap entry that the build found on a JAX-RS method.
 */
public final class SitemapEntryBuildItem extends MultiBuildItem {

    private final String path;
    private final String changefreq;
    private final double priority;

    /**
     * Create the entry.
     *
     * @param path the joined JAX-RS path, starting with a slash
     * @param changefreq the lower case change frequency, or an empty string when unset
     * @param priority the priority from 0.0 to 1.0, or -1 when unset
     */
    public SitemapEntryBuildItem(String path, String changefreq, double priority) {
        this.path = path;
        this.changefreq = changefreq;
        this.priority = priority;
    }

    /**
     * Return the path.
     *
     * @return the joined JAX-RS path, starting with a slash
     */
    public String path() {
        return path;
    }

    /**
     * Return the change frequency.
     *
     * @return the lower case change frequency, or an empty string when unset
     */
    public String changefreq() {
        return changefreq;
    }

    /**
     * Return the priority.
     *
     * @return the priority from 0.0 to 1.0, or -1 when unset
     */
    public double priority() {
        return priority;
    }
}
