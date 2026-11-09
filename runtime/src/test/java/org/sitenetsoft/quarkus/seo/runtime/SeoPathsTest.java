package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SeoPathsTest {

    @Test
    void addsALeadingSlashAndTrims() {
        assertThat(SeoPaths.normalize(" sitemap.xml ")).isEqualTo("/sitemap.xml");
    }

    @Test
    void collapsesRepeatedSlashes() {
        assertThat(SeoPaths.normalize("//maps///sitemap.xml")).isEqualTo("/maps/sitemap.xml");
    }

    @Test
    void stripsTheTrailingSlashExceptForTheRoot() {
        assertThat(SeoPaths.normalize("/maps/")).isEqualTo("/maps");
        assertThat(SeoPaths.normalize("/")).isEqualTo("/");
        assertThat(SeoPaths.normalize("//")).isEqualTo("/");
        assertThat(SeoPaths.normalize("")).isEqualTo("/");
        assertThat(SeoPaths.normalize(null)).isEqualTo("/");
    }

    @Test
    void robotsSitemapLineUsesTheNormalizedPath() {
        assertThat(Urls.absolute("https://ex.com", SeoPaths.normalize("maps//sitemap.xml/")))
                .isEqualTo("https://ex.com/maps/sitemap.xml");
    }
}
