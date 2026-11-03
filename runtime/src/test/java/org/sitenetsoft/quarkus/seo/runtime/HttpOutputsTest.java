package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpOutputsTest {

    @Test
    void etagIsTheQuotedHashPrefix() {
        // SHA-256 of "abc" starts with ba7816bf8f01cfea.
        assertThat(HttpOutputs.etag("abc")).isEqualTo("\"ba7816bf8f01cfea\"");
    }

    @Test
    void ifNoneMatchRules() {
        String etag = "\"ba7816bf8f01cfea\"";
        assertThat(HttpOutputs.matches(null, etag)).isFalse();
        assertThat(HttpOutputs.matches("", etag)).isFalse();
        assertThat(HttpOutputs.matches(etag, etag)).isTrue();
        assertThat(HttpOutputs.matches("W/" + etag, etag)).isTrue();
        assertThat(HttpOutputs.matches("\"x\" , " + etag, etag)).isTrue();
        assertThat(HttpOutputs.matches("*", etag)).isTrue();
        assertThat(HttpOutputs.matches("\"x\"", etag)).isFalse();
    }
}
