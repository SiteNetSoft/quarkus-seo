package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapEntry;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class SitemapEndpointTest {

    static final AtomicBoolean CLOSED = new AtomicBoolean();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.sitemap.urls", "/,/about")
            .withApplicationRoot(jar -> jar.addClasses(Products.class, Blog.class));

    @Test
    void servesAllSources() {
        CLOSED.set(false);
        Response response = given().get("/sitemap.xml");
        response.then().statusCode(200);
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/xml; charset=utf-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("public, max-age=3600");
        String base = "http://localhost:" + RestAssured.port;
        assertThat(response.asString())
                .startsWith("<?xml")
                .contains("<loc>" + base + "/</loc>")
                .contains("<loc>" + base + "/about</loc>")
                .contains("<loc>" + base + "/products</loc><changefreq>weekly</changefreq>")
                .contains("<loc>" + base + "/blog/1</loc><lastmod>2026-01-02T03:04:05Z</lastmod>");
        assertThat(CLOSED).isTrue();
    }

    @Test
    void matchingEtagGetsNotModified() {
        String etag = given().get("/sitemap.xml").then().statusCode(200).extract().header("ETag");
        assertThat(etag).matches("\"[0-9a-f]{16}\"");
        String body = given().header("If-None-Match", etag).get("/sitemap.xml")
                .then().statusCode(304).extract().asString();
        assertThat(body).isEmpty();
        given().header("If-None-Match", "\"other\", " + etag).get("/sitemap.xml").then().statusCode(304);
        given().header("If-None-Match", "*").get("/sitemap.xml").then().statusCode(304);
        given().header("If-None-Match", "\"other\"").get("/sitemap.xml").then().statusCode(200);
    }

    @Path("/")
    public static class Products {

        @GET
        @Produces("text/plain")
        @Path("/products")
        @SitemapEntry(changefreq = "weekly")
        public String list() {
            return "products";
        }
    }

    @ApplicationScoped
    public static class Blog implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of(SitemapUrl.of("/blog/1").lastmod(Instant.parse("2026-01-02T03:04:05Z")))
                    .onClose(() -> CLOSED.set(true));
        }
    }
}
