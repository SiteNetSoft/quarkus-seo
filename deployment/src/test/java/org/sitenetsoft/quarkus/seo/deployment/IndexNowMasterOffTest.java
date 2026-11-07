package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.IndexNow;

class IndexNowMasterOffTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.enabled", "false")
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true");

    @Test
    void startsWithoutKeyAndRegistersNoBean() {
        assertThat(Arc.container().instance(IndexNow.class).isAvailable()).isFalse();
        given().basePath("").get("/sitemap.xml").then().statusCode(404);
    }
}
