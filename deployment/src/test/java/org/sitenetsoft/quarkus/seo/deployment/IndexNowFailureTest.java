package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.seo.IndexNow;

import io.quarkus.test.QuarkusUnitTest;

class IndexNowFailureTest {

    static final FakeIndexNowEndpoint FAKE = FakeIndexNowEndpoint.of(IndexNowFailureTest.class.getName());

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(FakeIndexNowEndpoint.class))
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .overrideConfigKey("quarkus.seo.indexnow.key", "abcd1234")
            .overrideConfigKey("quarkus.seo.indexnow.endpoint", FAKE.endpoint())
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com")
            .overrideConfigKey("quarkus.seo.indexable", "true");

    @Inject
    IndexNow indexNow;

    @Test
    void serverErrorCompletesNormally() throws Exception {
        FAKE.status.set(500);
        try {
            assertNonBlockingAndNormal(indexNow);
        } finally {
            FAKE.status.set(202);
        }
    }

    private static void assertNonBlockingAndNormal(IndexNow indexNow) throws Exception {
        long start = System.nanoTime();
        CompletableFuture<Void> stage = indexNow.submit("/a").toCompletableFuture();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(100);
        stage.get(15, TimeUnit.SECONDS);
        assertThat(stage.isCompletedExceptionally()).isFalse();
    }

    @AfterAll
    static void stopFake() {
        FAKE.stop();
    }
}
