package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class LlmsTxtEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.llms-txt.enabled", "true")
            .overrideConfigKey("quarkus.seo.site-name", "Ex")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class));

    @Test
    void servesTheLinkList() {
        Response response = given().basePath("").get("/llms.txt");
        response.then().statusCode(200);
        assertThat(response.getHeader("Content-Type")).isEqualTo("text/plain; charset=utf-8");
        assertThat(response.asString()).startsWith("# Ex\n")
                .contains("- [A page](http://localhost:" + RestAssured.port + "/a)");
    }

    @ApplicationScoped
    public static class Pages implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of(SitemapUrl.of("/a").title("A page"));
        }
    }
}
