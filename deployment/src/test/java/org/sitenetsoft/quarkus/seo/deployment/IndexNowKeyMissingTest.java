package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class IndexNowKeyMissingTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .assertException(t -> assertThat(t).hasMessageContaining("quarkus.seo.indexnow.key"));

    @Test
    void missingKeyFailsTheBuild() {
        fail("The build must fail");
    }
}
