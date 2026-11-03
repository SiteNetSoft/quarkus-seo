package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

class RobotsEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true");

    @Test
    void servesRobotsTxtWithHeaders() {
        Response response = given().basePath("").get("/robots.txt");
        response.then().statusCode(200).header("Content-Type", "text/plain; charset=utf-8")
                .header("Cache-Control", "public, max-age=3600");
        assertThat(response.header("ETag")).isNotBlank();
        assertThat(response.asString()).startsWith("User-agent: *\nAllow: /\n")
                .contains("Sitemap: http://localhost:").endsWith("/sitemap.xml\n");
    }

    @Test
    void answersNotModifiedForTheSameEtag() {
        String etag = given().basePath("").get("/robots.txt").header("ETag");
        given().basePath("").header("If-None-Match", etag).get("/robots.txt").then().statusCode(304);
    }
}
