package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;

class RouteSitemapConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .withApplicationRoot(jar -> jar.addClasses(Sitemap.class));

    @Test
    void servesTheApplicationRoute() {
        String body = given().basePath("").get("/sitemap.xml").then().statusCode(200).extract().asString();
        assertThat(body).isEqualTo("mine");
    }

    @Test
    void robotsStillListsTheSitemap() {
        String body = given().basePath("").get("/robots.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("Sitemap: http://localhost:").endsWith("/sitemap.xml\n");
    }

    public static class Sitemap {

        @Route(path = "/sitemap.xml", produces = "application/xml")
        public String sitemap() {
            return "mine";
        }
    }
}
