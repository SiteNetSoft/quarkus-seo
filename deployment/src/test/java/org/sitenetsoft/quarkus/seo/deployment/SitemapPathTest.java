package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class SitemapPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.sitemap.path", "/maps/site.xml")
            .overrideConfigKey("quarkus.seo.sitemap.page-path", "/maps/site-{n}.xml")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.seo.sitemap.urls", "/,/about");

    @Test
    void sitemapAnswersUnderTheRootPath() {
        String base = "http://localhost:" + RestAssured.port + "/app/";
        String body = given().basePath("").get("/app/maps/site.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("<loc>" + base + "</loc>").contains("<loc>" + base + "about</loc>");
        given().basePath("").get("/maps/site.xml").then().statusCode(404);
        given().basePath("").get("/app/sitemap.xml").then().statusCode(404);
    }

    @Test
    void pageAnswersUnderTheRootPath() {
        String body = given().basePath("").get("/app/maps/site-1.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("/app/about</loc>");
    }
}
