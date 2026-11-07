package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SecurityTxtInvalidExpiresTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.security-txt.enabled", "true")
            .overrideConfigKey("quarkus.seo.security-txt.contact", "mailto:sec@ex.com")
            .overrideConfigKey("quarkus.seo.security-txt.expires", "not-a-date")
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com");

    @Test
    void leavesOutTheExpiresLine() {
        String body = given().basePath("").get("/.well-known/security.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("Contact: mailto:sec@ex.com").doesNotContain("Expires:");
    }
}
