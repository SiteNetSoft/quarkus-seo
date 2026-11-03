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

    private static final String DEFAULT_EXPRESSION_MARK = "${user.language";
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
     * Read a locale key only when the application set it.
     *
     * <p>
     * Quarkus fills {@code quarkus.locales} with the build machine locale when no source sets it. The value comes
     * from the profile-aware config lookup. It counts as set when it differs from that default, or when a source
     * other than the one that won the lookup provides it. No source name is used.
     */
    private static List<String> explicitList(SmallRyeConfig config, String key) {
        ConfigValue value = config.getConfigValue(key);
        List<String> result = new ArrayList<>();
        if (value == null || value.getValue() == null || value.getValue().isBlank()) {
            return result;
        }
        boolean differs = !value.getValue().equals(machineDefault());
        if (!differs && !providedElsewhere(config, key, value.getConfigSourceOrdinal())) {
            return result;
        }
        for (String part : value.getValue().split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /**
     * Expand the Quarkus default of {@code quarkus.locales} on this JVM.
     */
    private static String machineDefault() {
        return System.getProperty("user.language", "en") + "-" + System.getProperty("user.country", "");
    }

    private static boolean providedElsewhere(SmallRyeConfig config, String key, int winningOrdinal) {
        for (ConfigSource source : config.getConfigSources()) {
            if (source.getOrdinal() == winningOrdinal) {
                continue;
            }
            for (String profile : config.getProfiles()) {
                if (source.getValue("%" + profile + "." + key) != null) {
                    return true;
                }
            }
            String raw = source.getValue(key);
            if (raw != null && !raw.contains(DEFAULT_EXPRESSION_MARK)) {
                return true;
            }
        }
        return false;
    }
}
