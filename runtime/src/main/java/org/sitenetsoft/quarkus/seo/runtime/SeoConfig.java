package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Set the site-wide defaults of the SEO output at run time.
 */
@ConfigMapping(prefix = "quarkus.seo")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SeoConfig {

    /**
     * Name the site. When unset, the value of {@code quarkus.application.name} applies.
     *
     * @return the site name
     */
    Optional<String> siteName();

    /**
     * Set the public URL of the application root. The URL must hold any public path prefix, because the extension
     * does not append {@code quarkus.http.root-path} to it. When unset, the request host and the root path apply.
     *
     * @return the site URL
     */
    Optional<String> siteUrl();

    /**
     * List the locales of the site. When unset, the value of {@code quarkus.locales} applies.
     *
     * @return the locale tags
     */
    Optional<List<String>> locales();

    /**
     * Set the default locale. When unset, the value of {@code quarkus.default-locale} applies.
     *
     * @return the locale tag
     */
    Optional<String> defaultLocale();

    /**
     * Let search engines index the site. When unset, the site is indexable in the prod profile only.
     *
     * @return the indexable flag
     */
    Optional<Boolean> indexable();

    /**
     * List the hosts accepted in absolute URLs.
     *
     * @return the hosts
     */
    Optional<List<String>> allowedHosts();

    /**
     * Configure the page title.
     *
     * @return the title group
     */
    Title title();

    /**
     * Set the default description.
     *
     * @return the description
     */
    Optional<String> description();

    /**
     * Set the default Open Graph image, as a relative or absolute URL.
     *
     * @return the image URL
     */
    Optional<String> image();

    /**
     * Set the alt text of the default image.
     *
     * @return the alt text
     */
    Optional<String> imageAlt();

    /**
     * Configure the Twitter card tags.
     *
     * @return the twitter group
     */
    Twitter twitter();

    /**
     * Configure the site verification tags.
     *
     * @return the verification group
     */
    Verification verification();

    /**
     * Set the theme-color meta tag.
     *
     * @return the color
     */
    Optional<String> themeColor();

    /**
     * Configure canonical URLs.
     *
     * @return the canonical group
     */
    Canonical canonical();

    /**
     * Configure the Organization JSON-LD node.
     *
     * @return the organization group
     */
    Organization organization();

    /**
     * Configure the WebSite JSON-LD node.
     *
     * @return the website group
     */
    Website website();

    /**
     * Configure the sitemap content.
     *
     * @return the sitemap group
     */
    Sitemap sitemap();

    /**
     * Configure the robots.txt content.
     *
     * @return the robots group
     */
    Robots robots();

    /**
     * Configure the security.txt content.
     *
     * @return the security.txt group
     */
    SecurityTxt securityTxt();

    /**
     * Configure the web manifest content.
     *
     * @return the manifest group
     */
    Manifest manifest();

    /**
     * Configure the IndexNow client.
     *
     * @return the IndexNow group
     */
    @WithName("indexnow")
    IndexNowConfig indexNow();

    /**
     * Choose which crawlers the AI preset blocks.
     */
    enum AiPreset {
        /** Add no rule for AI crawlers. */
        ALLOW,
        /** Disallow the training crawlers and keep the search crawlers. */
        BLOCK_TRAINING,
        /** Disallow the training, search and user-request crawlers. */
        BLOCK_ALL
    }

    /**
     * Keys of the page title.
     */
    interface Title {

        /**
         * Set the title pattern. The token {@code %s} stands for the page title and {@code {site-name}} for the site name.
         *
         * @return the pattern
         */
        Optional<String> template();

        /**
         * Set the title of a page that sets none. When unset, the site name applies.
         *
         * @return the title
         */
        @WithName("default")
        Optional<String> defaultTitle();
    }

    /**
     * Keys of the Twitter card tags.
     */
    interface Twitter {

        /**
         * Set the twitter:site handle.
         *
         * @return the handle
         */
        Optional<String> site();

        /**
         * Set the twitter:creator handle.
         *
         * @return the handle
         */
        Optional<String> creator();
    }

    /**
     * Keys of the site verification tags.
     */
    interface Verification {

        /**
         * Set the Google verification token.
         *
         * @return the token
         */
        Optional<String> google();

        /**
         * Set the Bing verification token.
         *
         * @return the token
         */
        Optional<String> bing();

        /**
         * Set the Yandex verification token.
         *
         * @return the token
         */
        Optional<String> yandex();
    }

    /**
     * Keys of canonical URLs.
     */
    interface Canonical {

        /**
         * List the query parameters that stay in canonical URLs. All other parameters are removed.
         *
         * @return the parameter names
         */
        Optional<List<String>> keepQueryParams();
    }

    /**
     * Keys of the Organization JSON-LD node.
     */
    interface Organization {

        /**
         * Write the Organization node.
         *
         * @return true when the node is written
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Set the organization name.
         *
         * @return the name
         */
        Optional<String> name();

        /**
         * Set the organization URL.
         *
         * @return the URL
         */
        Optional<String> url();

        /**
         * Set the organization logo, as a relative or absolute URL.
         *
         * @return the logo URL
         */
        Optional<String> logo();

        /**
         * List the profile URLs of the organization.
         *
         * @return the profile URLs
         */
        Optional<List<String>> sameAs();
    }

    /**
     * Keys of the WebSite JSON-LD node.
     */
    interface Website {

        /**
         * Write the WebSite node.
         *
         * @return true when the node is written
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Keys of the sitemap content.
     */
    interface Sitemap {

        /**
         * List the paths added to the sitemap.
         *
         * @return the paths
         */
        Optional<List<String>> urls();

        /**
         * Set the number of URLs above which the sitemap becomes an index.
         *
         * @return the maximum number of URLs per sitemap
         */
        @WithDefault("50000")
        int maxUrls();

        /**
         * Set the Cache-Control header of the sitemap.
         *
         * @return the header value
         */
        @WithDefault("public, max-age=3600")
        String cacheControl();

        /**
         * Configure the sitemap cache.
         *
         * @return the cache group
         */
        Cache cache();
    }

    /**
     * Keys of the robots.txt content.
     */
    interface Robots {

        /**
         * Define rule groups by name.
         *
         * @return the groups
         */
        Map<String, RobotsGroupConfig> groups();

        /**
         * Choose the AI crawler preset.
         *
         * @return the preset
         */
        @WithDefault("allow")
        AiPreset ai();

        /**
         * Configure the crawler token lists of the AI preset.
         *
         * @return the AI agents group
         */
        AiAgents aiAgents();

        /**
         * Configure the robots.txt cache.
         *
         * @return the cache group
         */
        Cache cache();
    }

    /**
     * Keys of one robots.txt rule group.
     */
    interface RobotsGroupConfig {

        /**
         * List the crawlers the group applies to.
         *
         * @return the user agents
         */
        Optional<List<String>> userAgents();

        /**
         * List the paths the group allows.
         *
         * @return the paths
         */
        Optional<List<String>> allow();

        /**
         * List the paths the group disallows.
         *
         * @return the paths
         */
        Optional<List<String>> disallow();

        /**
         * Set the crawl delay in seconds.
         *
         * @return the delay
         */
        Optional<Integer> crawlDelay();
    }

    /**
     * Keys that replace the crawler token lists of the AI preset.
     */
    interface AiAgents {

        /**
         * List the training crawlers. The list replaces the built-in one.
         *
         * @return the crawler tokens
         */
        Optional<List<String>> training();

        /**
         * List the search and user-request crawlers. The list replaces the built-in one.
         *
         * @return the crawler tokens
         */
        Optional<List<String>> search();
    }

    /**
     * Keys of the security.txt content.
     */
    interface SecurityTxt {

        /**
         * List the contact URIs. A value without a scheme that holds an at sign gets the {@code mailto:} scheme.
         * Without a contact, the path answers 404.
         *
         * @return the contacts
         */
        Optional<List<String>> contact();

        /**
         * Set the expiry date as an ISO 8601 date or date-time. A value that does not parse is left out.
         *
         * @return the expiry
         */
        Optional<String> expires();

        /**
         * Set the URL of the encryption key.
         *
         * @return the key URL
         */
        Optional<String> encryption();

        /**
         * Set the URL of the disclosure policy.
         *
         * @return the policy URL
         */
        Optional<String> policy();

        /**
         * List the preferred languages as language tags.
         *
         * @return the language tags
         */
        Optional<List<String>> preferredLanguages();

        /**
         * Set the URL of the hiring page.
         *
         * @return the hiring URL
         */
        Optional<String> hiring();
    }

    /**
     * Keys of the IndexNow client.
     */
    interface IndexNowConfig {

        /**
         * Set the IndexNow endpoint that receives the submissions.
         *
         * @return the endpoint URL
         */
        @WithDefault("https://api.indexnow.org/indexnow")
        String endpoint();
    }

    /**
     * Choose the display mode of the web manifest.
     */
    enum Display {
        /** Use the whole screen. */
        FULLSCREEN,
        /** Look like a standalone application. */
        STANDALONE,
        /** Look like a standalone application with a minimal set of browser controls. */
        MINIMAL_UI,
        /** Open in a browser tab. */
        BROWSER
    }

    /**
     * Keys of the web manifest content.
     */
    interface Manifest {

        /**
         * Set the application name. When unset, the site name applies.
         *
         * @return the name
         */
        Optional<String> name();

        /**
         * Set the short application name.
         *
         * @return the short name
         */
        Optional<String> shortName();

        /**
         * Set the start URL. A value that starts with a slash gets the HTTP root path in front. When unset, the
         * start URL is the root path.
         *
         * @return the start URL
         */
        Optional<String> startUrl();

        /**
         * Set the display mode.
         *
         * @return the display mode
         */
        Optional<Display> display();

        /**
         * Set the theme color. When unset, {@code quarkus.seo.theme-color} applies.
         *
         * @return the color
         */
        Optional<String> themeColor();

        /**
         * Set the background color.
         *
         * @return the color
         */
        Optional<String> backgroundColor();

        /**
         * Define icons by name. The manifest lists them in the order of their names.
         *
         * @return the icons
         */
        Map<String, Icon> icons();
    }

    /**
     * Keys of one web manifest icon.
     */
    interface Icon {

        /**
         * Set the icon URL. A value that starts with a slash gets the HTTP root path in front.
         *
         * @return the URL
         */
        String src();

        /**
         * Set the icon sizes, for example {@code 192x192}.
         *
         * @return the sizes
         */
        Optional<String> sizes();

        /**
         * Set the media type of the icon.
         *
         * @return the media type
         */
        Optional<String> type();
    }

    /**
     * Keys of an output cache.
     */
    interface Cache {

        /**
         * Store the generated output in a named cache. The cache needs {@code quarkus-cache}.
         *
         * @return true when the output is cached
         */
        @WithDefault("false")
        boolean enabled();
    }
}
