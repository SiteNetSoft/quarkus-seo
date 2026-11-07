package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ManifestIconWithoutSrcTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.manifest.icons.x.sizes", "192x192");

    @Test
    void startsWithTheManifestOff() {
        given().basePath("").get("/manifest.webmanifest").then().statusCode(404);
    }
}
