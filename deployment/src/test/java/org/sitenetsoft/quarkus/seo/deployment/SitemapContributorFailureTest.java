package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class SitemapContributorFailureTest {

    static final AtomicBoolean CLOSED = new AtomicBoolean();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Broken.class))
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
            .assertLogRecords(records -> {
                assertThat(records).hasSize(1);
                assertThat(records.get(0).getMessage()).contains(Broken.class.getName());
            });

    @Test
    void failingContributorGivesAnEmptyServerError() {
        String body = get("/sitemap.xml").then().statusCode(500).extract().asString();
        assertThat(body).isEmpty();
        assertThat(CLOSED).isTrue();
    }

    @ApplicationScoped
    public static class Broken implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of("/x", "/y").map(Broken::fail).onClose(() -> CLOSED.set(true));
        }

        private static SitemapUrl fail(String path) {
            throw new IllegalStateException("database is down");
        }
    }
}
