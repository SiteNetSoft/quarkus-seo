package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateException;

class SeoSectionHelperFactoryTest {

    private static final Engine ENGINE = Engine.builder().addDefaults()
            .addSectionHelper(new SeoSectionHelperFactory()).build();

    @Test
    void unknownParameterNamesTheParameterAndTheKnownNames() {
        assertThatThrownBy(() -> ENGINE.parse("{#seo titel=\"x\" /}"))
                .isInstanceOf(TemplateException.class)
                .hasMessageContaining("The seo tag has no parameter [titel]")
                .hasMessageContaining("Known parameters: [title, absoluteTitle,");
    }

    @Test
    void positionalParameterIsUnknown() {
        assertThatThrownBy(() -> ENGINE.parse("{#seo \"x\" /}"))
                .isInstanceOf(TemplateException.class)
                .hasMessageContaining("has no parameter [\"x\"]");
    }

    @Test
    void knownParametersParse() {
        ENGINE.parse("{#seo title=\"a\" noindex=true imageWidth=10 locale=\"fr-CA\" /}");
    }

    @Test
    void falseOrBlankFlagsAreNotSet() {
        Map<String, Object> values = new HashMap<>();
        values.put("noindex", false);
        values.put("nofollow", "");
        values.put("noimageindex", "false");
        SeoValues tag = SeoSectionHelperFactory.tagValues(values);
        assertThat(tag.noindex()).isNull();
        assertThat(tag.nofollow()).isNull();
        assertThat(tag.noimageindex()).isNull();
    }

    @Test
    void trueFlagsAreSet() {
        SeoValues tag = SeoSectionHelperFactory.tagValues(Map.of("noindex", true, "nofollow", "true"));
        assertThat(tag.noindex()).isTrue();
        assertThat(tag.nofollow()).isTrue();
    }

    @Test
    void imageSizeAcceptsNumbersAndNumericText() {
        SeoValues tag = SeoSectionHelperFactory.tagValues(Map.of("imageWidth", 1200, "imageHeight", " 630 "));
        assertThat(tag.imageWidth()).isEqualTo(1200);
        assertThat(tag.imageHeight()).isEqualTo(630);
    }

    @Test
    void imageSizeIgnoresOtherText() {
        SeoValues tag = SeoSectionHelperFactory.tagValues(Map.of("imageWidth", "wide"));
        assertThat(tag.imageWidth()).isNull();
    }

    @Test
    void localeIsALanguageTag() {
        assertThat(SeoSectionHelperFactory.tagValues(Map.of("locale", "fr-CA")).locale())
                .isEqualTo(Locale.CANADA_FRENCH);
        assertThat(SeoSectionHelperFactory.tagValues(Map.of("locale", " ")).locale()).isNull();
    }
}
