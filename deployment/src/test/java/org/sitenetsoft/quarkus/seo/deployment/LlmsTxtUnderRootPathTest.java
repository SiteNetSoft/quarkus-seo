package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class LlmsTxtUnderRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .overrideConfigKey("quarkus.seo.llms-txt.enabled", "true")
            .overrideConfigKey("quarkus.seo.llms-txt.serve-under-root-path", "true")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class));

    @Test
    void llmsTxtMovesUnderTheRootPath() {
        given().basePath("").get("/app/llms.txt").then().statusCode(200);
        given().basePath("").get("/llms.txt").then().statusCode(404);
    }

    @ApplicationScoped
    public static class Pages implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of(SitemapUrl.of("/a").title("A page"));
        }
    }
}
