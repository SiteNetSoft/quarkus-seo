package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SecurityTxtRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.security-txt.enabled", "true")
            .overrideConfigKey("quarkus.seo.security-txt.contact", "mailto:sec@ex.com")
            .overrideConfigKey("quarkus.http.root-path", "/app");

    @Test
    void securityTxtStaysAtTheHostRoot() {
        String body = given().basePath("").get("/.well-known/security.txt").then().statusCode(200).extract()
                .asString();
        assertThat(body).contains("Canonical: http://localhost:").endsWith("/.well-known/security.txt\n")
                .doesNotContain("/app");
        given().basePath("").get("/app/.well-known/security.txt").then().statusCode(404);
    }
}
