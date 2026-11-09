package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class RobotsUnderRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.seo.robots.serve-under-root-path", "true");

    @Test
    void robotsMovesUnderTheRootPath() {
        String body = given().basePath("").get("/app/robots.txt").then().statusCode(200).extract().asString();
        assertThat(body).endsWith("/app/sitemap.xml\n");
        given().basePath("").get("/robots.txt").then().statusCode(404);
    }
}
