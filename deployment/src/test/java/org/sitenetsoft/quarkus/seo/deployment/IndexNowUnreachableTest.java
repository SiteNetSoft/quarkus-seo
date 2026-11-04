package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.seo.IndexNow;

import io.quarkus.test.QuarkusUnitTest;

class IndexNowUnreachableTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .overrideConfigKey("quarkus.seo.indexnow.key", "abcd1234")
            .overrideConfigKey("quarkus.seo.indexnow.endpoint", "http://localhost:1/x")
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com")
            .overrideConfigKey("quarkus.seo.indexable", "true");

    @Inject
    IndexNow indexNow;

    @Test
    void closedPortCompletesNormally() throws Exception {
        assertNonBlockingAndNormal(indexNow);
    }

    private static void assertNonBlockingAndNormal(IndexNow indexNow) throws Exception {
        long start = System.nanoTime();
        CompletableFuture<Void> stage = indexNow.submit("/a").toCompletableFuture();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(100);
        stage.get(15, TimeUnit.SECONDS);
        assertThat(stage.isCompletedExceptionally()).isFalse();
    }
}
