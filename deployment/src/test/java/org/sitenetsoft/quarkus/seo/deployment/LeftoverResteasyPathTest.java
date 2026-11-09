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

class LeftoverResteasyPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.resteasy.path", "/legacy")
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .withApplicationRoot(jar -> jar.addClasses(Products.class));

    @Test
    void leftoverResteasyPathDoesNotPrefixSitemapEntries() {
        String base = "http://localhost:" + RestAssured.port;
        String body = given().basePath("").get("/sitemap.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("<loc>" + base + "/products</loc>");
        assertThat(body).doesNotContain("/legacy");
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
}
