package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.sitenetsoft.quarkus.seo.SitemapEntry;

class RestPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.rest.path", "/api")
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .withApplicationRoot(jar -> jar.addClasses(Products.class, Robots.class));

    @Test
    void sitemapEntryTakesTheRestPath() {
        String base = "http://localhost:" + RestAssured.port;
        String body = given().basePath("").get("/sitemap.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("<loc>" + base + "/api/products</loc>");
        given().basePath("").get("/api/products").then().statusCode(200);
    }

    @Test
    void resourceUnderTheRestPathDoesNotOwnRobotsTxt() {
        String body = given().basePath("").get("/robots.txt").then().statusCode(200).extract().asString();
        assertThat(body).startsWith("User-agent: *");
        assertThat(given().basePath("").get("/api/robots.txt").then().statusCode(200).extract().asString())
                .isEqualTo("mine");
    }

    @Path("/products")
    public static class Products {

        @GET
        @Produces("text/plain")
        @SitemapEntry
        public String list() {
            return "products";
        }
    }

    @Path("/robots.txt")
    public static class Robots {

        @GET
        @Produces("text/plain")
        public String robots() {
            return "mine";
        }
    }
}
