package org.sitenetsoft.quarkus.seo.deployment;

import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Hold the outputs that the extension registers after the switches and the paths of the application.
 */
public final class SeoOutputsBuildItem extends SimpleBuildItem {

    private final boolean meta;
    private final boolean sitemap;
    private final boolean robots;
    private final boolean securityTxt;
    private final boolean manifest;
    private final boolean indexNow;
    private final boolean llmsTxt;
    private final String indexNowKeyPath;
    private final Set<String> ownedByApplication;

    /**
     * Create the build item.
     *
     * @param meta true when the extension registers the meta tag feature
     * @param sitemap true when the extension serves the sitemap
     * @param robots true when the extension serves robots.txt
     * @param securityTxt true when the extension serves security.txt
     * @param manifest true when the extension serves the web manifest
     * @param indexNow true when the extension serves the IndexNow key file
     * @param llmsTxt true when the extension serves llms.txt
     * @param indexNowKeyPath the path of the IndexNow key file, or null when IndexNow is off
     * @param ownedByApplication the output paths that the application serves itself
     */
    SeoOutputsBuildItem(boolean meta, boolean sitemap, boolean robots,
            boolean securityTxt, boolean manifest, boolean indexNow, boolean llmsTxt, String indexNowKeyPath,
            Set<String> ownedByApplication) {
        this.meta = meta;
        this.sitemap = sitemap;
        this.robots = robots;
        this.securityTxt = securityTxt;
        this.manifest = manifest;
        this.indexNow = indexNow;
        this.llmsTxt = llmsTxt;
        this.indexNowKeyPath = indexNowKeyPath;
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
     * Tell whether the extension serves security.txt.
     *
     * @return true when the security.txt feature is switched on and the application does not own its path
     */
    public boolean securityTxt() {
        return securityTxt;
    }

    /**
     * Tell whether the extension serves the web manifest.
     *
     * @return true when the manifest feature is switched on and the application does not own its path
     */
    public boolean manifest() {
        return manifest;
    }

    /**
     * Tell whether the extension serves the IndexNow key file.
     *
     * @return true when the IndexNow feature is switched on and the application does not own the key path
     */
    public boolean indexNow() {
        return indexNow;
    }

    /**
     * Tell whether the extension serves llms.txt.
     *
     * @return true when the llms.txt feature is switched on and the application does not own its path
     */
    public boolean llmsTxt() {
        return llmsTxt;
    }

    /**
     * Return the path of the IndexNow key file.
     *
     * @return the path {@code /{key}.txt}, or empty when the extension does not serve IndexNow
     */
    public Optional<String> indexNowKeyPath() {
        return indexNow ? Optional.of(indexNowKeyPath) : Optional.empty();
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
