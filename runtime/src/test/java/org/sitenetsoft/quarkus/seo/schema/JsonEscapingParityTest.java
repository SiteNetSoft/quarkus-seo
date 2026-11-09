package org.sitenetsoft.quarkus.seo.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sitenetsoft.quarkus.seo.runtime.Json;

/**
 * The schema package is JDK-only, so it keeps its own escape routine. This test keeps it equal to the runtime copy.
 */
class JsonEscapingParityTest {

    @ParameterizedTest
    @ValueSource(strings = { "", "plain", "a\"b", "back\\slash", "</script>", "\u0000\u0001\u001f", "\n\r\t\b\f",
            "\u2028\u2029", "é😀" })
    void bothWritersEscapeAlike(String text) {
        StringBuilder schema = new StringBuilder();
        StringBuilder runtime = new StringBuilder();
        JsonWriter.string(schema, text);
        Json.string(runtime, text);
        assertThat(runtime).hasToString(schema.toString());
    }
}
