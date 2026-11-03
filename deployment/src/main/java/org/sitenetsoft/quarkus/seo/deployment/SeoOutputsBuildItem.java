package org.sitenetsoft.quarkus.seo.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Hold the outputs that the extension registers after the switches and the paths of the application.
 */
public final class SeoOutputsBuildItem extends SimpleBuildItem {

    private final boolean meta;
    private final boolean sitemap;
    private final boolean robots;
    private final Set<String> ownedByApplication;

    /**
     * Create the build item.
     *
     * @param meta true when the extension registers the meta tag feature
     * @param sitemap true when the extension serves the sitemap
     * @param robots true when the extension serves robots.txt
     * @param ownedByApplication the output paths that the application serves itself
     */
    SeoOutputsBuildItem(boolean meta, boolean sitemap, boolean robots, Set<String> ownedByApplication) {
        this.meta = meta;
        this.sitemap = sitemap;
        this.robots = robots;
        this.ownedByApplication = Set.copyOf(ownedByApplication);
    }

    /**
     * Tell whether the extension registers the {@code Seo} bean, the {@code {#seo}} tag and the header filter.
     *
     * @return true when the meta tag feature is registered
     */
    public boolean meta() {
        return meta;
    }

    /**
     * Tell whether the extension serves the sitemap.
     *
     * @return true when the sitemap feature is switched on and the application does not own the sitemap path
     */
    public boolean sitemap() {
        return sitemap;
    }

    /**
     * Tell whether the extension serves robots.txt.
     *
     * @return true when the robots.txt feature is switched on and the application does not own robots.txt
     */
    public boolean robots() {
        return robots;
    }

    /**
     * Return the output paths that the application serves itself.
     *
     * @return the paths, such as {@code /robots.txt}
     */
    public Set<String> ownedByApplication() {
        return ownedByApplication;
    }
}
