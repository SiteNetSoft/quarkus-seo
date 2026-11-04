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

class LlmsTxtRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.seo.llms-txt.enabled", "true")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class));

    @Test
    void linksStartWithTheRootPath() {
        String body = given().basePath("").get("/llms.txt").then().statusCode(200).extract().asString();
        assertThat(body).contains("](http://localhost:" + RestAssured.port + "/app/a)");
    }

    @ApplicationScoped
    public static class Pages implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of(SitemapUrl.of("/a").title("A page"));
        }
    }
}
