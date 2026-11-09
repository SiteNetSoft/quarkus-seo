package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class FeatureSwitchSitemapTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .overrideConfigKey("quarkus.seo.sitemap.enabled", "false");

    @Test
    void servesNoSitemap() {
        given().basePath("").get("/sitemap.xml").then().statusCode(404);
    }

    @Test
    void servesRobotsWithoutSitemapLine() {
        String body = given().basePath("").get("/robots.txt").then().statusCode(200).extract().asString();
        assertThat(body).doesNotContain("Sitemap:");
    }
}
