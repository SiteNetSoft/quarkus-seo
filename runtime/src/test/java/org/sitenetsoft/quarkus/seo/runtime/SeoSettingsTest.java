package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class SeoSettingsTest {

    @Test
    void indexableOnlyInProdByDefault() {
        assertThat(SeoSettings.indexable(Optional.empty(), List.of("prod"))).isTrue();
        assertThat(SeoSettings.indexable(Optional.empty(), List.of("dev"))).isFalse();
        assertThat(SeoSettings.indexable(Optional.of(true), List.of("test"))).isTrue();
        assertThat(SeoSettings.indexable(Optional.of(false), List.of("prod"))).isFalse();
    }

    @Test
    void titleTemplateTakesTheSiteName() {
        SeoSettings settings = new SeoSettings(Optional.of("Acme"), true, "%s | {site-name}", Optional.empty(), "");
        assertThat(settings.titleTemplate()).isEqualTo("%s | Acme");
        assertThat(settings.siteName()).contains("Acme");
    }

    @Test
    void titleTemplateWithoutSiteNameKeepsOnlyThePlaceholder() {
        SeoSettings settings = new SeoSettings(Optional.empty(), true, null, Optional.empty(), "");
        assertThat(settings.titleTemplate()).isEqualTo("%s");
    }

    @Test
    void defaultTitleFallsBackToTheSiteName() {
        SeoSettings settings = new SeoSettings(Optional.of("Acme"), true, null, Optional.empty(), "");
        assertThat(settings.defaultTitle()).contains("Acme");
        SeoSettings explicit = new SeoSettings(Optional.of("Acme"), true, null, Optional.of("Home"), "");
        assertThat(explicit.defaultTitle()).contains("Home");
    }

    @Test
    void rootPathHasNoTrailingSlash() {
        assertThat(SeoSettings.normalizeRootPath("/")).isEmpty();
        assertThat(SeoSettings.normalizeRootPath("/app/")).isEqualTo("/app");
        assertThat(SeoSettings.normalizeRootPath("app")).isEqualTo("/app");
    }
}
