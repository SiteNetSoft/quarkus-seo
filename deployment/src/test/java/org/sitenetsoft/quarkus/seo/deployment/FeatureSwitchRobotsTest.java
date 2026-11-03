package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class FeatureSwitchRobotsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.robots.enabled", "false");

    @Test
    void servesNoRobotsTxt() {
        given().basePath("").get("/robots.txt").then().statusCode(404);
    }

    @Test
    void stillServesTheSitemap() {
        given().basePath("").get("/sitemap.xml").then().statusCode(200);
    }
}
