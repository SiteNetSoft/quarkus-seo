package org.sitenetsoft.quarkus.seo.runtime.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitenetsoft.quarkus.seo.runtime.devui.PageFetcher.localPath;

import org.junit.jupiter.api.Test;

class PageFetcherTest {

    @Test
    void acceptsPlainPaths() {
        assertThat(localPath("/a/b?x=1")).contains("/a/b?x=1");
        assertThat(localPath("/")).contains("/");
    }

    @Test
    void refusesOtherHosts() {
        assertThat(localPath("https://evil.example/")).isEmpty();
        assertThat(localPath("//evil.example/x")).isEmpty();
        assertThat(localPath("x")).isEmpty();
        assertThat(localPath("/\\evil.example")).isEmpty();
        assertThat(localPath("/a\r\nHost: evil")).isEmpty();
    }

    @Test
    void refusesBlankAndSpacedValues() {
        assertThat(localPath(null)).isEmpty();
        assertThat(localPath("")).isEmpty();
        assertThat(localPath("/a b")).isEmpty();
        assertThat(localPath("/a\tb")).isEmpty();
        assertThat(localPath("/a b")).isEmpty();
        assertThat(localPath("/a\u0000")).isEmpty();
    }

    @Test
    void capturePathDropsQueryAndFragment() {
        assertThat(PageFetcher.capturePath("/a/b?x=1#top")).isEqualTo("/a/b");
        assertThat(PageFetcher.capturePath("/a#top")).isEqualTo("/a");
        assertThat(PageFetcher.capturePath("/a")).isEqualTo("/a");
    }
}
