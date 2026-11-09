package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class LlmsTxtFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.llms-txt.enabled", "true")
            .withApplicationRoot(jar -> jar.addClasses(Broken.class))
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
            .assertLogRecords(records -> assertThat(records).isNotEmpty());

    @Test
    void failingContributorGivesAnEmptyServerError() {
        String body = given().basePath("").get("/llms.txt").then().statusCode(500).extract().asString();
        assertThat(body).isEmpty();
    }

    @ApplicationScoped
    public static class Broken implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            throw new IllegalStateException("database is down");
        }
    }
}
