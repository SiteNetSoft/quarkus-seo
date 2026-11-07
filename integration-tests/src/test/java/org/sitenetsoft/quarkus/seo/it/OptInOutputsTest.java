package org.sitenetsoft.quarkus.seo.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(FakeIndexNowResource.class)
class OptInOutputsTest {

    @Test
    void securityTxt() {
        String body = given().get("/.well-known/security.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("Contact: mailto:security@example.org")
                .contains("Expires: 2030-01-01")
                .contains("Canonical: https://example.org/.well-known/security.txt");
    }

    @Test
    void manifest() {
        given().get("/manifest.webmanifest").then().statusCode(200)
                .contentType("application/manifest+json")
                .body(containsString("\"name\":\"Example\""));
    }

    @Test
    void indexNowKeyFile() {
        String body = given().get("/sample-key-1234.txt").then().statusCode(200).extract().asString();
        assertThat(body.trim()).isEqualTo("sample-key-1234");
    }

    @Test
    void llmsTxt() {
        String body = given().get("/llms.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("[First post](https://example.org/blog/first)")
                .contains("[Second post](https://example.org/blog/second)");
    }

    @Test
    void headHasManifestLink() {
        String body = given().get("/").then().statusCode(200).extract().asString();
        assertThat(body).contains("rel=\"manifest\"").contains("/manifest.webmanifest");
    }

    @Test
    void publishSubmitsToIndexNow() {
        given().post("/publish").then().statusCode(200);
        String received = given().get(System.getProperty(FakeIndexNowResource.URL_PROPERTY)).asString();
        assertThat(received).contains("https://example.org/blog/first").contains("sample-key-1234");
    }
}
