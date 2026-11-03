package org.sitenetsoft.quarkus.seo.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import org.sitenetsoft.quarkus.seo.SeoLocales;

/**
 * Read the locales from the SEO keys and then from the Quarkus locale keys.
 */
@DefaultBean
@Singleton
public class DefaultSeoLocales implements SeoLocales {

    private static final Locale FALLBACK = Locale.forLanguageTag("en-US");

    private final List<Locale> locales;
    private final Locale defaultLocale;

    /**
     * Resolve the locales once at startup.
     *
     * @param config the SEO configuration
     * @param quarkusKeys the Quarkus locale keys that the application set, as the build saw them
     */
    @Inject
    public DefaultSeoLocales(SeoConfig config, QuarkusLocaleKeys quarkusKeys) {
        this(config.locales().orElse(List.of()), config.defaultLocale(), quarkusKeys);
    }

    /**
     * Resolve the locales from plain values.
     *
     * @param seoLocales the tags of {@code quarkus.seo.locales}, empty when unset
     * @param seoDefault the tag of {@code quarkus.seo.default-locale}
     * @param quarkusKeys the Quarkus locale keys that the application set
     */
    DefaultSeoLocales(List<String> seoLocales, Optional<String> seoDefault, QuarkusLocaleKeys quarkusKeys) {
        List<String> tags = seoLocales.isEmpty() ? quarkusKeys.locales() : seoLocales;
        List<Locale> parsed = new ArrayList<>();
        for (String tag : tags) {
            if (!tag.isBlank()) {
                parsed.add(Locale.forLanguageTag(tag.trim()));
            }
        }
        if (parsed.isEmpty()) {
            parsed.add(FALLBACK);
        }
        Optional<String> defaultTag = seoDefault.filter(value -> !value.isBlank());
        if (defaultTag.isEmpty()) {
            defaultTag = quarkusKeys.defaultLocale();
        }
        this.locales = List.copyOf(parsed);
        this.defaultLocale = defaultTag.isPresent() ? Locale.forLanguageTag(defaultTag.get().trim()) : parsed.get(0);
    }

    @Override
    public List<Locale> locales() {
        return locales;
    }

    @Override
    public Locale defaultLocale() {
        return defaultLocale;
    }
}
