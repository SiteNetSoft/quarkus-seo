package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ManifestIconWithoutSrcServedTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.manifest.enabled", "true")
            .overrideConfigKey("quarkus.seo.manifest.icons.x.sizes", "192x192");

    @Test
    void servesTheManifestWithoutTheIcon() {
        String body = given().basePath("").get("/manifest.webmanifest").then().statusCode(200).extract().asString();
        assertThat(body).doesNotContain("192x192").contains("start_url");
    }
}
