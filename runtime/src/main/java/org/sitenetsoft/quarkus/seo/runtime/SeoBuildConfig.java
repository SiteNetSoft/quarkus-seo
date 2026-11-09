package org.sitenetsoft.quarkus.seo.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Switch features on or off and set output paths at build time.
 */
@ConfigMapping(prefix = "quarkus.seo")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface SeoBuildConfig {

    /**
     * Turn every SEO feature on or off.
     *
     * @return true when the features are registered
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Configure the meta tag feature.
     *
     * @return the meta group
     */
    MetaBuild meta();

    /**
     * Configure the JSON-LD feature.
     *
     * @return the JSON-LD group
     */
    JsonLdBuild jsonLd();

    /**
     * Configure the sitemap feature.
     *
     * @return the sitemap group
     */
    SitemapBuild sitemap();

    /**
     * Configure the robots.txt feature.
     *
     * @return the robots group
     */
    RobotsBuild robots();

    /**
     * Configure the security.txt feature.
     *
     * @return the security.txt group
     */
    SecurityTxtBuild securityTxt();

    /**
     * Configure the web manifest feature.
     *
     * @return the manifest group
     */
    ManifestBuild manifest();

    /**
     * Configure the IndexNow feature.
     *
     * @return the IndexNow group
     */
    @WithName("indexnow")
    IndexNowBuild indexNow();

    /**
     * Configure the llms.txt feature.
     *
     * @return the llms.txt group
     */
    LlmsTxtBuild llmsTxt();

    /**
     * Build-time keys of the meta tag feature.
     */
    interface MetaBuild {

        /**
         * Register the {@code {#seo}} tag, the {@code Seo} bean and the X-Robots-Tag filter.
         *
         * @return true when the meta feature is registered
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Build-time keys of the JSON-LD feature.
     */
    interface JsonLdBuild {

        /**
         * Write JSON-LD in the {@code {#seo}} tag.
         *
         * @return true when JSON-LD is written
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Build-time keys of the sitemap feature.
     */
    interface SitemapBuild {

        /**
         * Register the sitemap route.
         *
         * @return true when the sitemap is registered
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Set the path of the sitemap or of the sitemap index.
         *
         * @return the path
         */
        @WithDefault("/sitemap.xml")
        String path();

        /**
         * Set the path pattern of the sitemap pages. The token {@code {n}} stands for the page number.
         *
         * @return the path pattern
         */
        @WithDefault("/sitemap-{n}.xml")
        String pagePath();
    }

    /**
     * Build-time keys of the robots.txt feature.
     */
    interface RobotsBuild {

        /**
         * Register the robots.txt route.
         *
         * @return true when robots.txt is registered
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Serve robots.txt under the HTTP root path instead of the host root.
         *
         * @return true when the file is served under the root path
         */
        @WithDefault("false")
        boolean serveUnderRootPath();
    }

    /**
     * Build-time keys of the security.txt feature.
     */
    interface SecurityTxtBuild {

        /**
         * Register the security.txt route.
         *
         * @return true when security.txt is registered
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Serve security.txt under the HTTP root path instead of the host root.
         *
         * @return true when the file is served under the root path
         */
        @WithDefault("false")
        boolean serveUnderRootPath();
    }

    /**
     * Build-time keys of the web manifest feature.
     */
    interface ManifestBuild {

        /**
         * Register the web manifest route.
         *
         * @return true when the manifest is registered
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Set the path of the web manifest.
         *
         * @return the path
         */
        @WithDefault("/manifest.webmanifest")
        String path();
    }

    /**
     * Build-time keys of the IndexNow feature.
     */
    interface IndexNowBuild {

        /**
         * Register the IndexNow key file route and the IndexNow client.
         *
         * @return true when IndexNow is registered
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Set the IndexNow key. The key holds 8 to 128 letters, digits or dashes.
         *
         * @return the key, absent when not set
         */
        Optional<String> key();

        /**
         * Serve the key file under the HTTP root path instead of the host root.
         *
         * @return true when the file is served under the root path
         */
        @WithDefault("false")
        boolean serveUnderRootPath();
    }

    /**
     * Build-time keys of the llms.txt feature.
     */
    interface LlmsTxtBuild {

        /**
         * Register the llms.txt route.
         *
         * @return true when llms.txt is registered
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Serve llms.txt under the HTTP root path instead of the host root.
         *
         * @return true when the file is served under the root path
         */
        @WithDefault("false")
        boolean serveUnderRootPath();
    }
}
