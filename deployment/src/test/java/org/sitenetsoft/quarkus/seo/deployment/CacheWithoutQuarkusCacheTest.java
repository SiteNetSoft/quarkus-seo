package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.SeoCaches;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class CacheWithoutQuarkusCacheTest {

    static final AtomicInteger CALLS = new AtomicInteger();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Counting.class))
            .overrideConfigKey("quarkus.seo.sitemap.cache.enabled", "true")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.INFO.intValue()
                    && r.getMessage().contains("quarkus-cache is not present"))
            .assertLogRecords(records -> {
                assertThat(records).hasSize(1);
                assertThat(records.get(0).getMessage()).isEqualTo(
                        "quarkus.seo cache is enabled but quarkus-cache is not present. Serving without a cache.");
            });

    @Inject
    SeoCaches caches;

    @Test
    void servesWithoutACache() {
        CALLS.set(0);
        get("/sitemap.xml").then().statusCode(200);
        get("/sitemap.xml").then().statusCode(200);
        assertThat(CALLS).hasValue(2);
    }

    @Test
    void invalidationIsANoOp() {
        assertThatCode(() -> {
            caches.invalidateSitemap();
            caches.invalidateRobots();
        }).doesNotThrowAnyException();
    }

    @ApplicationScoped
    public static class Counting implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            CALLS.incrementAndGet();
            return Stream.of(SitemapUrl.of("/a"));
        }
    }
}
