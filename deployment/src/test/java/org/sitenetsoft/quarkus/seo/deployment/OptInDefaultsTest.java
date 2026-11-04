package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class OptInDefaultsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void optInOutputsAreOffByDefault() {
        given().basePath("").get("/.well-known/security.txt").then().statusCode(404);
        given().basePath("").get("/manifest.webmanifest").then().statusCode(404);
        given().basePath("").get("/llms.txt").then().statusCode(404);
    }
}
