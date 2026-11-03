package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class RequestInfoTest {

    @Test
    void pathIsRelativeToTheRootPath() {
        RequestInfo info = RequestInfo.of("https://ex.com/app", "/app/about", "a=1", "/app");
        assertThat(info.path()).isEqualTo("/about");
        assertThat(info.home()).isFalse();
        assertThat(RequestInfo.of("https://ex.com/app", "/app", null, "/app").home()).isTrue();
        assertThat(RequestInfo.of("https://ex.com", "/", null, "").home()).isTrue();
    }

    @Test
    void noActiveRequestGivesEmpty() {
        SeoSettings settings = new SeoSettings(Optional.empty(), true, null, Optional.empty(), "");
        assertThat(RequestInfo.current(context -> "https://ex.com", settings)).isEmpty();
    }
}
