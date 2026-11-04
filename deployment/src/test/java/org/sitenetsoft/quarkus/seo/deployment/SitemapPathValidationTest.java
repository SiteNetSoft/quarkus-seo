package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SitemapPathValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.sitemap.page-path", "/site map-{n}.xml")
            .assertException(t -> assertThat(t).hasMessageContaining("quarkus.seo.sitemap.page-path"));

    @Test
    void invalidPathFailsTheBuild() {
        fail("The build must fail");
    }
}
