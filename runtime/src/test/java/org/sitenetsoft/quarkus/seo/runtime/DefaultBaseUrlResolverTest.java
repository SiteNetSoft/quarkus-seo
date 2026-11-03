package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DefaultBaseUrlResolverTest {

    @Test
    void unsafeHostFallsBackToTheServerHostAndPort() {
        assertThat(DefaultBaseUrlResolver.choose("http", "evil.example/<x>", List.of(), "", () -> "localhost:8080"))
                .isEqualTo("http://localhost:8080");
    }

    @Test
    void missingHostFallsBackToTheServerHostAndPort() {
        assertThat(DefaultBaseUrlResolver.choose("http", null, List.of(), "/app", () -> "localhost:8080"))
                .isEqualTo("http://localhost:8080/app");
    }

    @Test
    void unsafeHostFallsBackToTheFirstAllowedHost() {
        assertThat(DefaultBaseUrlResolver.choose("https", "a\"b", List.of("ex.com"), "", () -> "localhost:8080"))
                .isEqualTo("https://ex.com");
    }

    @Test
    void safeHostIsKeptWithTheRootPath() {
        assertThat(DefaultBaseUrlResolver.choose("https", "Ex.com:8443", List.of("ex.com"), "/app", () -> "x"))
                .isEqualTo("https://Ex.com:8443/app");
    }
}
