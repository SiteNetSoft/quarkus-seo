package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SitemapTakesOverFromRoqTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .overrideConfigKey("quarkus.seo.sitemap.on-conflict", "take-over")
            .withApplicationRoot(jar -> RoqMarker.add(jar.addClass(RoqMarker.class)));

    @Test
    void servesTheSitemap() {
        String body = given().basePath("").get("/sitemap.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("<urlset");
    }
}
