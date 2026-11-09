package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class LlmsTxtWithoutSitemapTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.sitemap.enabled", "false")
            .overrideConfigKey("quarkus.seo.llms-txt.enabled", "true")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class));

    @Test
    void servesLlmsTxtButNoSitemap() {
        given().get("/sitemap.xml").then().statusCode(404);
        String body = given().basePath("").get("/llms.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("- [A page](http://localhost:" + RestAssured.port + "/a)");
    }

    @ApplicationScoped
    public static class Pages implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of(SitemapUrl.of("/a").title("A page"));
        }
    }
}
