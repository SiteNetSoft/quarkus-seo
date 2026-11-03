package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class SeoValuesTest {

    @Test
    void higherLayerReplacesOnlyTheFieldsItSets() {
        SeoValues lower = new SeoValues().title("A").image("/a.png");
        SeoValues merged = new SeoValues().title("B").over(lower);
        assertThat(merged.title()).isEqualTo("B");
        assertThat(merged.image()).isEqualTo("/a.png");
    }

    @Test
    void titleAndAbsoluteTitleAreOneGroup() {
        SeoValues merged = new SeoValues().title("B").over(new SeoValues().absoluteTitle("A"));
        assertThat(merged.title()).isEqualTo("B");
        assertThat(merged.absoluteTitle()).isNull();
    }

    @Test
    void imageFieldsAreOneGroup() {
        SeoValues lower = new SeoValues().image("/a.png").imageWidth(1200).imageHeight(630).imageAlt("A");
        SeoValues merged = new SeoValues().image("/b.png").over(lower);
        assertThat(merged.image()).isEqualTo("/b.png");
        assertThat(merged.imageWidth()).isNull();
        assertThat(merged.imageHeight()).isNull();
        assertThat(merged.imageAlt()).isNull();
    }

    @Test
    void listsAddLowerFirst() {
        SeoValues merged = new SeoValues().alternate(Locale.FRENCH, "/fr")
                .over(new SeoValues().alternate(Locale.ENGLISH, "/en"));
        assertThat(merged.alternates()).containsExactly(entry(Locale.ENGLISH, "/en"), entry(Locale.FRENCH, "/fr"));
    }

    @Test
    void blankCountsAsNotSet() {
        assertThat(new SeoValues().title("  ").over(new SeoValues().title("A")).title()).isEqualTo("A");
    }
}
