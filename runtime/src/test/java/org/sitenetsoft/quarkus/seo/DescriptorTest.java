package org.sitenetsoft.quarkus.seo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class DescriptorTest {

    @Test
    void descriptorProvidesTheCapability() throws IOException {
        Properties p = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/META-INF/quarkus-extension.properties")) {
            p.load(in);
        }
        assertThat(p.getProperty("provides-capabilities")).isEqualTo("org.sitenetsoft.seo");
    }
}
