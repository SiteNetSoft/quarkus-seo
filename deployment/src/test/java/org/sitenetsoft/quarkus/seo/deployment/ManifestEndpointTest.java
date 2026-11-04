package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

class ManifestEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.manifest.enabled", "true")
            .overrideConfigKey("quarkus.seo.site-name", "Ex");

    @Test
    void servesTheManifest() {
        Response response = given().basePath("").get("/manifest.webmanifest");
        response.then().statusCode(200).header("Content-Type", "application/manifest+json");
        assertThat(response.header("ETag")).isNotBlank();
        assertThat(response.asString()).contains("\"name\":\"Ex\"");
    }
}
