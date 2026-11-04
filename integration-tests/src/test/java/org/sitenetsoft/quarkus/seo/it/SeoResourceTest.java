package org.sitenetsoft.quarkus.seo.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SeoResourceTest {

    @Test
    void home() {
        String body = given().get("/").then().statusCode(200).extract().asString();
        assertThat(body).contains("<title>Example</title>")
                .contains("\"@type\":\"Organization\"")
                .contains("\"@type\":\"WebSite\"")
                .contains("<link rel=\"canonical\" href=\"https://example.org/\">");
    }

    @Test
    void page() {
        String body = given().get("/hello").then().statusCode(200).extract().asString();
        assertThat(body).contains("<title>Hello | Example</title>")
                .contains("hreflang=\"en\"")
                .contains("hreflang=\"fr\"")
                .contains("hreflang=\"x-default\"")
                .contains("content=\"https://example.org/img/hello.png\"")
                .contains("article:published_time")
                .contains("\"@type\":\"Article\"");
    }

    @Test
    void sitemap() {
        invalidate();
        String body = given().get("/sitemap.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("<loc>https://example.org/</loc>")
                .contains("<loc>https://example.org/blog/first</loc>")
                .contains("<loc>https://example.org/blog/second</loc>")
                .contains("<lastmod>2026-01-15T10:00:00Z</lastmod>")
                .contains("<lastmod>2026-02-20T10:00:00Z</lastmod>");
    }

    @Test
    void sitemapIsCached() {
        invalidate();
        int before = calls();
        given().get("/sitemap.xml").then().statusCode(200);
        given().get("/sitemap.xml").then().statusCode(200);
        assertThat(calls()).isEqualTo(before + 1);
    }

    @Test
    void invalidateClearsTheCache() {
        invalidate();
        given().get("/sitemap.xml").then().statusCode(200);
        int before = calls();
        invalidate();
        given().get("/sitemap.xml").then().statusCode(200);
        assertThat(calls()).isEqualTo(before + 1);
    }

    @Test
    void invalidateFromEventLoop() {
        invalidate();
        int start = calls();
        given().get("/sitemap.xml").then().statusCode(200);
        assertThat(calls()).isEqualTo(start + 1);
        given().get("/sitemap.xml").then().statusCode(200);
        assertThat(calls()).isEqualTo(start + 1);

        given().post("/invalidate-on-event-loop").then().statusCode(200);

        given().get("/sitemap.xml").then().statusCode(200);
        assertThat(calls()).isEqualTo(start + 2);
    }

    @Test
    void robots() {
        String body = given().get("/robots.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("User-agent: GPTBot")
                .contains("Sitemap: https://example.org/sitemap.xml");
    }

    private static void invalidate() {
        given().post("/invalidate").then().statusCode(200);
    }

    private static int calls() {
        return Integer.parseInt(given().get("/contributor-calls").then().statusCode(200).extract().asString());
    }
}
