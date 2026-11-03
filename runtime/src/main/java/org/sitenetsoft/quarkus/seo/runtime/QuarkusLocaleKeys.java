package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.Optional;

/**
 * Hold the Quarkus locale keys that the application set, as the build saw them.
 */
public final class QuarkusLocaleKeys {

    private final List<String> locales;
    private final String defaultLocale;

    /**
     * Create the holder.
     *
     * @param locales the tags of {@code quarkus.locales}, empty when the application did not set the key
     * @param defaultLocale the tag of {@code quarkus.default-locale}, empty when the application did not set the key
     */
    public QuarkusLocaleKeys(List<String> locales, String defaultLocale) {
        this.locales = List.copyOf(locales);
        this.defaultLocale = defaultLocale == null ? "" : defaultLocale;
    }

    /**
     * Return the tags of {@code quarkus.locales}.
     *
     * @return the tags, empty when the application did not set the key
     */
    public List<String> locales() {
        return locales;
    }

    /**
     * Return the tag of {@code quarkus.default-locale}.
     *
     * @return the tag, or empty when the application did not set the key
     */
    public Optional<String> defaultLocale() {
        return defaultLocale.isBlank() ? Optional.empty() : Optional.of(defaultLocale);
    }
}
