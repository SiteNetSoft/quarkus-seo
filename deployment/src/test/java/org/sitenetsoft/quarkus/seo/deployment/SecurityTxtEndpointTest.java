package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

class SecurityTxtEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.security-txt.enabled", "true")
            .overrideConfigKey("quarkus.seo.security-txt.contact", "mailto:sec@ex.com")
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com");

    @Test
    void servesSecurityTxt() {
        Response response = given().basePath("").get("/.well-known/security.txt");
        response.then().statusCode(200).header("Content-Type", "text/plain; charset=utf-8");
        assertThat(response.header("ETag")).isNotBlank();
        assertThat(response.asString()).startsWith("Contact: mailto:sec@ex.com\n")
                .endsWith("Canonical: https://ex.com/.well-known/security.txt\n");
    }
}
