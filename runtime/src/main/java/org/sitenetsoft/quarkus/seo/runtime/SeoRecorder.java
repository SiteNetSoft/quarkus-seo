package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Record build-time findings for the run time.
 */
@Recorder
public class SeoRecorder {

    /**
     * Create the supplier of the Quarkus locale keys.
     *
     * @param locales the tags of {@code quarkus.locales}, empty when the application did not set the key
     * @param defaultLocale the tag of {@code quarkus.default-locale}, empty when the application did not set the key
     * @return the supplier of the holder
     */
    public Supplier<QuarkusLocaleKeys> localeKeys(List<String> locales, String defaultLocale) {
        QuarkusLocaleKeys keys = new QuarkusLocaleKeys(locales, defaultLocale);
        return () -> keys;
    }
}
