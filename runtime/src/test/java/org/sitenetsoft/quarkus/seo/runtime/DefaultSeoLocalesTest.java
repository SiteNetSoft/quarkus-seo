package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class DefaultSeoLocalesTest {

    @Test
    void noKeysGivesEnUs() {
        DefaultSeoLocales locales = new DefaultSeoLocales(List.of(), Optional.empty(), new QuarkusLocaleKeys(List.of(), ""));
        assertThat(locales.locales()).containsExactly(Locale.forLanguageTag("en-US"));
        assertThat(locales.defaultLocale()).isEqualTo(Locale.forLanguageTag("en-US"));
    }

    @Test
    void quarkusKeysApplyWhenSeoKeysAreUnset() {
        DefaultSeoLocales locales = new DefaultSeoLocales(List.of(), Optional.empty(),
                new QuarkusLocaleKeys(List.of("fr", "en"), "en"));
        assertThat(locales.locales()).containsExactly(Locale.forLanguageTag("fr"), Locale.forLanguageTag("en"));
        assertThat(locales.defaultLocale()).isEqualTo(Locale.forLanguageTag("en"));
    }

    @Test
    void seoKeysWin() {
        DefaultSeoLocales locales = new DefaultSeoLocales(List.of("de"), Optional.of("de"),
                new QuarkusLocaleKeys(List.of("fr"), "fr"));
        assertThat(locales.locales()).containsExactly(Locale.forLanguageTag("de"));
        assertThat(locales.defaultLocale()).isEqualTo(Locale.forLanguageTag("de"));
    }
}
