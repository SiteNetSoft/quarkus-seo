package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class SitemapSplitTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.sitemap.urls", "/a,/b,/c,/d,/e")
            .overrideRuntimeConfigKey("quarkus.seo.sitemap.max-urls", "2");

    @Test
    void sitemapBecomesAnIndex() {
        String base = "http://localhost:" + RestAssured.port;
        String body = get("/sitemap.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("<sitemapindex").doesNotContain("<urlset");
        assertThat(body.split("<loc>", -1)).hasSize(4);
        assertThat(body).contains("<loc>" + base + "/sitemap-1.xml</loc>")
                .contains("<loc>" + base + "/sitemap-2.xml</loc>")
                .contains("<loc>" + base + "/sitemap-3.xml</loc>");
    }

    @Test
    void pagesHoldTheirSlice() {
        String first = get("/sitemap-1.xml").then().statusCode(200).extract().asString();
        assertThat(first).contains("/a</loc>").contains("/b</loc>").doesNotContain("/c</loc>");
        String last = get("/sitemap-3.xml").then().statusCode(200).extract().asString();
        assertThat(last.split("<url>", -1)).hasSize(2);
        assertThat(last).contains("/e</loc>");
    }

    @Test
    void missingPagesAreNotFound() {
        get("/sitemap-4.xml").then().statusCode(404);
        get("/sitemap-0.xml").then().statusCode(404);
        get("/sitemap-01.xml").then().statusCode(404);
        get("/sitemap-99999999999999999999.xml").then().statusCode(404);
    }
}
