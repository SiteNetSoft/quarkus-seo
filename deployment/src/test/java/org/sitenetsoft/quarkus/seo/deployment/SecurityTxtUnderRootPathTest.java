package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SecurityTxtUnderRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.seo.security-txt.enabled", "true")
            .overrideConfigKey("quarkus.seo.security-txt.contact", "mailto:sec@ex.com")
            .overrideConfigKey("quarkus.seo.security-txt.serve-under-root-path", "true");

    @Test
    void securityTxtMovesUnderTheRootPath() {
        String body = given().basePath("").get("/app/.well-known/security.txt").then().statusCode(200).extract()
                .asString();
        assertThat(body).contains("Canonical: http://localhost:").endsWith("/app/.well-known/security.txt\n");
        given().basePath("").get("/.well-known/security.txt").then().statusCode(404);
    }
}
