package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.Seo;

class SiteUrlRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.seo.site-url", "https://example.org/app")
            .overrideConfigKey("quarkus.seo.sitemap.urls", "/about")
            .withApplicationRoot(jar -> jar.addClasses(About.class));

    @Test
    void canonicalHoldsTheRootPathOnce() {
        String body = given().basePath("").get("/app/about").then().statusCode(200).extract().asString();
        assertThat(body).isEqualTo("https://example.org/app/about");
    }

    @Test
    void sitemapLocHoldsTheRootPathOnce() {
        String body = given().basePath("").get("/app/sitemap.xml").then().statusCode(200).extract().asString();
        assertThat(body).contains("<loc>https://example.org/app/about</loc>").doesNotContain("/app/app/");
    }

    @Path("/about")
    @Produces("text/plain")
    public static class About {

        @Inject
        Seo seo;

        @GET
        public String about() {
            return seo.resolve().canonical().orElse("");
        }
    }
}
