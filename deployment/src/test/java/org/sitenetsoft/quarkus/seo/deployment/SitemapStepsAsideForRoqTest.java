package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SitemapStepsAsideForRoqTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .withApplicationRoot(jar -> RoqMarker.add(jar.addClass(RoqMarker.class)));

    @Test
    void servesNoSitemap() {
        given().basePath("").get("/sitemap.xml").then().statusCode(404);
    }

    @Test
    void robotsStillListsTheSitemap() {
        String body = given().basePath("").get("/robots.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("Sitemap: http://localhost:").endsWith("/sitemap.xml\n");
    }
}
