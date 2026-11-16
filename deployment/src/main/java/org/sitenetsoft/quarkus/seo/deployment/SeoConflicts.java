package org.sitenetsoft.quarkus.seo.deployment;

import java.util.List;

import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig.OnConflict;

/**
 * Find what else in the application provides a feature of this extension.
 *
 * <p>
 * The checks look at the build class path only, so the extension depends on no other extension.
 */
final class SeoConflicts {

    /** The template suffixes that Qute tries for a user tag. */
    private static final List<String> TAG_SUFFIXES = List.of("", ".html", ".htm", ".txt", ".xml", ".json", ".qute.html",
            ".qute.txt");

    /** The template paths of a {@code seo} user tag, as a regular expression for Qute's path exclusion. */
    static final String SEO_TAG_PATHS = "tags/seo(\\.[^/]*)?";

    /** A class of Roq's sitemap plugin. Roq declares no capability, so the class tells that the plugin is present. */
    static final String ROQ_SITEMAP_CLASS = "io/quarkiverse/roq/plugin/sitemap/runtime/RoqSitemapKeys.class";

    private SeoConflicts() {
    }

    /**
     * Tell whether the meta feature steps aside: another {@code seo} tag exists and the meta conflict rule is
     * {@code step-aside}.
     *
     * @param config the build-time configuration
     * @return true when the extension must not register its tag
     */
    static boolean metaStepsAside(SeoBuildConfig config) {
        return config.meta().onConflict() == OnConflict.STEP_ASIDE && hasSeoTag(classLoader());
    }

    /**
     * Tell whether the sitemap steps aside: Roq's sitemap plugin is present and the sitemap conflict rule is
     * {@code step-aside}.
     *
     * @param config the build-time configuration
     * @return true when the extension must not register its sitemap
     */
    static boolean sitemapStepsAside(SeoBuildConfig config) {
        return config.sitemap().onConflict() == OnConflict.STEP_ASIDE && hasRoqSitemap(classLoader());
    }

    /**
     * Tell whether the class path holds a {@code templates/tags/seo} template.
     *
     * @param loader the class loader to search
     * @return true when a {@code seo} user tag exists
     */
    static boolean hasSeoTag(ClassLoader loader) {
        for (String suffix : TAG_SUFFIXES) {
            if (loader.getResource("templates/tags/seo" + suffix) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell whether Roq's sitemap plugin is on the class path.
     *
     * @param loader the class loader to search
     * @return true when the plugin exists
     */
    static boolean hasRoqSitemap(ClassLoader loader) {
        return loader.getResource(ROQ_SITEMAP_CLASS) != null;
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null ? loader : SeoConflicts.class.getClassLoader();
    }
}
