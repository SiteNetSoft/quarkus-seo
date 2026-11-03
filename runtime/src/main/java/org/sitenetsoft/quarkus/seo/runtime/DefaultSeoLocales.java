package org.sitenetsoft.quarkus.seo.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.arc.DefaultBean;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import org.sitenetsoft.quarkus.seo.SeoLocales;

/**
 * Read the locales from the SEO keys and then from the Quarkus locale keys.
 */
@DefaultBean
@Singleton
public class DefaultSeoLocales implements SeoLocales {

    private static final String FIXED_SOURCE = "BuildTime RunTime Fixed";
    private static final String DEFAULTS_SOURCE = "DefaultValuesConfigSource";
    private static final Locale FALLBACK = Locale.forLanguageTag("en-US");

    private final List<Locale> locales;
    private final Locale defaultLocale;

    /**
     * Resolve the locales once at startup.
     *
     * @param config the SEO configuration
     */
    @Inject
    public DefaultSeoLocales(SeoConfig config) {
        SmallRyeConfig quarkusConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        List<String> tags = new ArrayList<>();
        if (config.locales().isPresent()) {
            tags.addAll(config.locales().get());
        } else {
            tags.addAll(explicitList(quarkusConfig, "quarkus.locales"));
        }
        List<Locale> parsed = new ArrayList<>();
        for (String tag : tags) {
            if (!tag.isBlank()) {
                parsed.add(Locale.forLanguageTag(tag.trim()));
            }
        }
        if (parsed.isEmpty()) {
            parsed.add(FALLBACK);
        }
        String defaultTag = config.defaultLocale().filter(value -> !value.isBlank()).orElse(null);
        if (defaultTag == null) {
            List<String> quarkusDefault = explicitList(quarkusConfig, "quarkus.default-locale");
            defaultTag = quarkusDefault.isEmpty() ? null : quarkusDefault.get(0);
        }
        this.locales = List.copyOf(parsed);
        this.defaultLocale = defaultTag == null ? parsed.get(0) : Locale.forLanguageTag(defaultTag.trim());
    }

    @Override
    public List<Locale> locales() {
        return locales;
    }

    @Override
    public Locale defaultLocale() {
        return defaultLocale;
    }

    /**
     * Read a locale key only when a developer set it.
     *
     * <p>
     * Quarkus fills both keys with a default taken from the build machine locale. The fixed build-time source
     * holds that default and an explicit value alike, so the check looks at the other sources instead.
     */
    private static List<String> explicitList(SmallRyeConfig config, String key) {
        if (!isExplicit(config, key)) {
            return List.of();
        }
        ConfigValue value = config.getConfigValue(key);
        List<String> result = new ArrayList<>();
        if (value == null || value.getValue() == null) {
            return result;
        }
        for (String part : value.getValue().split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private static boolean isExplicit(SmallRyeConfig config, String key) {
        for (ConfigSource source : config.getConfigSources()) {
            String name = source.getName();
            if (FIXED_SOURCE.equals(name) || DEFAULTS_SOURCE.equals(name)) {
                continue;
            }
            if (source.getValue(key) != null) {
                return true;
            }
        }
        return false;
    }
}
