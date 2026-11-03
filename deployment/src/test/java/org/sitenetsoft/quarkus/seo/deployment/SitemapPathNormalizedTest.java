package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class SitemapPathNormalizedTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.sitemap.path", " maps//site.xml/ ")
            .overrideConfigKey("quarkus.seo.sitemap.page-path", "maps//site-{n}.xml")
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .overrideConfigKey("quarkus.seo.sitemap.urls", "/a,/b")
            .overrideRuntimeConfigKey("quarkus.seo.sitemap.max-urls", "1");

    @Test
    void everyOutputUsesTheNormalizedPaths() {
        String base = "http://localhost:" + RestAssured.port;
        String index = given().basePath("").get("/maps/site.xml").then().statusCode(200).extract().asString();
        assertThat(index).contains("<loc>" + base + "/maps/site-1.xml</loc>")
                .contains("<loc>" + base + "/maps/site-2.xml</loc>");
        given().basePath("").get("/maps/site-1.xml").then().statusCode(200);
        String robots = given().basePath("").get("/robots.txt").then().statusCode(200).extract().asString();
        assertThat(robots).contains("Sitemap: " + base + "/maps/site.xml\n");
    }
}
