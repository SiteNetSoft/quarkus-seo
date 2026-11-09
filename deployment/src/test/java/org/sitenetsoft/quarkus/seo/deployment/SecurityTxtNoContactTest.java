package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SecurityTxtNoContactTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.security-txt.enabled", "true");

    @Test
    void answersNotFoundWithoutContact() {
        given().basePath("").get("/.well-known/security.txt").then().statusCode(404);
    }
}
