package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class UnknownTagParameterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource(new StringAsset("<head>{#seo titel=\"x\" /}</head>"), "templates/typo.html"))
            .assertException(t -> assertThat(t).hasMessageContaining("titel").hasMessageContaining("title"));

    @Test
    void unknownParameterFailsTheBuild() {
        fail("The build must fail");
    }
}
