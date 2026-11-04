package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.seo.IndexNow;

import io.quarkus.test.QuarkusUnitTest;

class IndexNowSubmitTest {

    static final FakeIndexNowEndpoint FAKE = FakeIndexNowEndpoint.of(IndexNowSubmitTest.class.getName());

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

    @BeforeEach
    void clear() {
        FAKE.bodies.clear();
        FAKE.contentTypes.clear();
    }

    @Test
    void postsTheUrlsAsJson() throws Exception {
        indexNow.submit("/a", "/b").toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(FAKE.bodies).hasSize(1);
        assertThat(FAKE.bodies.get(0)).contains("\"urlList\":[\"https://ex.com/a\",\"https://ex.com/b\"]")
                .contains("\"key\":\"abcd1234\"");
        assertThat(FAKE.contentTypes).isEqualTo(List.of("application/json; charset=utf-8"));
    }

    @AfterAll
    static void stopFake() {
        FAKE.stop();
    }
}
