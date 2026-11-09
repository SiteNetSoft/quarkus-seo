package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.seo.IndexNow;

import io.quarkus.test.QuarkusUnitTest;

class IndexNowNotIndexableTest {

    static final FakeIndexNowEndpoint FAKE = FakeIndexNowEndpoint.of(IndexNowNotIndexableTest.class.getName());

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(FakeIndexNowEndpoint.class))
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .overrideConfigKey("quarkus.seo.indexnow.key", "abcd1234")
            .overrideConfigKey("quarkus.seo.indexnow.endpoint", FAKE.endpoint())
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com")
            .overrideConfigKey("quarkus.seo.indexable", "false");

    @Inject
    IndexNow indexNow;

    @BeforeEach
    void clear() {
        FAKE.bodies.clear();
        FAKE.contentTypes.clear();
    }

    @Test
    void sendsNothingWhenNotIndexable() throws Exception {
        indexNow.submit("/a").toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(FAKE.bodies).isEmpty();
    }

    @AfterAll
    static void stopFake() {
        FAKE.stop();
    }
}
