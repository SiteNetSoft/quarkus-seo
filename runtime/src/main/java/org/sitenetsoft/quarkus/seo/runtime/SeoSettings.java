package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Hold the resolved site settings that other beans share.
 */
@Singleton
public class SeoSettings {

    private static final String SITE_NAME_TOKEN = "{site-name}";

    private final Optional<String> siteName;
    private final boolean indexable;
    private final String titleTemplate;
    private final Optional<String> defaultTitle;
    private final String rootPath;

    /**
     * Read the settings from the configuration.
     *
     * @param config the SEO configuration
     */
    @Inject
    public SeoSettings(SeoConfig config) {
        this(resolveSiteName(config),
                indexable(config.indexable(), ConfigUtils.getProfiles()),
                config.title().template().orElse(null),
                config.title().defaultTitle(),
                normalizeRootPath(ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.http.root-path", String.class).orElse("/")));
    }

    /**
     * Build the settings from resolved values.
     *
     * @param siteName the site name
     * @param indexable true when search engines may index the site
     * @param titleTemplate the configured title template, or null for the default
     * @param defaultTitle the configured default title
     * @param rootPath the HTTP root path
     */
    public SeoSettings(Optional<String> siteName, boolean indexable, String titleTemplate,
            Optional<String> defaultTitle, String rootPath) {
        this.siteName = siteName;
        this.indexable = indexable;
        String template = titleTemplate;
        if (template == null || template.isBlank()) {
            template = siteName.isPresent() ? "%s | " + SITE_NAME_TOKEN : "%s";
        }
        this.titleTemplate = template.replace(SITE_NAME_TOKEN, siteName.orElse(""));
        this.defaultTitle = defaultTitle.isPresent() ? defaultTitle : siteName;
        this.rootPath = normalizeRootPath(rootPath);
    }

    /**
     * Return the site name.
     *
     * @return the site name, or empty when none is set
     */
    public Optional<String> siteName() {
        return siteName;
    }

    /**
     * Tell whether search engines may index the site.
     *
     * @return true when the site is indexable
     */
    public boolean indexable() {
        return indexable;
    }

    /**
     * Return the title template with the site name filled in.
     *
     * @return the template, where {@code %s} stands for the page title
     */
    public String titleTemplate() {
        return titleTemplate;
    }

    /**
     * Return the title of a page that sets none.
     *
     * @return the default title, or empty when there is none
     */
    public Optional<String> defaultTitle() {
        return defaultTitle;
    }

    /**
     * Return the HTTP root path.
     *
     * @return the root path with no trailing slash, or an empty string for {@code /}
     */
    public String rootPath() {
        return rootPath;
    }

    /**
     * Decide whether the site is indexable.
     *
     * @param configured the value of {@code quarkus.seo.indexable}
     * @param activeProfiles the active Quarkus profiles
     * @return the configured value, or true in the prod profile and false otherwise
     */
    public static boolean indexable(Optional<Boolean> configured, List<String> activeProfiles) {
        return configured.orElse(activeProfiles.contains("prod"));
    }

    static String normalizeRootPath(String value) {
        if (value == null) {
            return "";
        }
        String path = value.trim();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static Optional<String> resolveSiteName(SeoConfig config) {
        Optional<String> configured = config.siteName().filter(value -> !value.isBlank());
        if (configured.isPresent()) {
            return configured;
        }
        return ConfigProvider.getConfig().getOptionalValue("quarkus.application.name", String.class)
                .filter(value -> !value.isBlank());
    }
}
