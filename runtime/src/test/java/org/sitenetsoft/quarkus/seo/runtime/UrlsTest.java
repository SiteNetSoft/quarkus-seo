package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class UrlsTest {

    @Test
    void keepsAbsoluteHttpUrls() {
        assertThat(Urls.absolute("https://ex.com", "http://cdn.ex.com/a.png")).isEqualTo("http://cdn.ex.com/a.png");
    }

    @Test
    void joinsRelativePathsWithOneSlash() {
        assertThat(Urls.absolute("https://ex.com/app", "/en/about")).isEqualTo("https://ex.com/app/en/about");
        assertThat(Urls.absolute("https://ex.com/app", "en/about")).isEqualTo("https://ex.com/app/en/about");
    }

    @Test
    void schemeRelativeTakesTheBaseScheme() {
        assertThat(Urls.absolute("https://ex.com", "//cdn.ex.com/a")).isEqualTo("https://cdn.ex.com/a");
    }

    @Test
    void canonicalQueryKeepsListedParamsInRequestOrder() {
        assertThat(Urls.canonicalQuery("utm_source=x&page=2&lang=fr", List.of("lang", "page"))).isEqualTo("page=2&lang=fr");
        assertThat(Urls.canonicalQuery("utm_source=x", List.of())).isEmpty();
    }

    @Test
    void refusesHostsWithPathOrMarkup() {
        assertThat(Urls.isSafeHost("ex.com:8443")).isTrue();
        assertThat(Urls.isSafeHost("[::1]:8080")).isTrue();
        assertThat(Urls.isSafeHost("ex.com/evil")).isFalse();
        assertThat(Urls.isSafeHost("ex.com\"><script>")).isFalse();
        assertThat(Urls.isSafeHost("")).isFalse();
    }
}
