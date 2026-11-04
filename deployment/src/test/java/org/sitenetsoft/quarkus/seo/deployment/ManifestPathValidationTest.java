package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ManifestPathValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.manifest.enabled", "true")
            .overrideConfigKey("quarkus.seo.manifest.path", "/a:b")
            .assertException(t -> assertThat(t).hasMessageContaining("quarkus.seo.manifest.path"));

    @Test
    void invalidPathFailsTheBuild() {
        fail("The build must fail");
    }
}
