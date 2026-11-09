package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

class IndexNowKeyFileTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .overrideConfigKey("quarkus.seo.indexnow.key", "abcd1234")
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com");

    @Test
    void servesTheKey() {
        Response response = given().basePath("").get("/abcd1234.txt");
        response.then().statusCode(200).header("Content-Type", "text/plain; charset=utf-8");
        assertThat(response.asString()).isEqualTo("abcd1234");
    }
}
