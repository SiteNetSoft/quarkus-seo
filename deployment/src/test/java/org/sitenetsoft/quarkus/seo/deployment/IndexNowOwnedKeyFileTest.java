package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.seo.IndexNow;

import io.quarkus.test.QuarkusUnitTest;

class IndexNowOwnedKeyFileTest {

    static final FakeIndexNowEndpoint FAKE = FakeIndexNowEndpoint.of(IndexNowOwnedKeyFileTest.class.getName());

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(FakeIndexNowEndpoint.class)
                    .addAsResource(new StringAsset("mine"), "META-INF/resources/abcd1234.txt"))
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .overrideConfigKey("quarkus.seo.indexnow.key", "abcd1234")
            .overrideConfigKey("quarkus.seo.indexnow.endpoint", FAKE.endpoint())
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com")
            .overrideConfigKey("quarkus.seo.indexable", "true");

    @Inject
    IndexNow indexNow;

    @Test
    void theApplicationFileWinsAndSubmitStillWorks() throws Exception {
        assertThat(given().basePath("").get("/abcd1234.txt").then().statusCode(200).extract().asString())
                .isEqualTo("mine");
        indexNow.submit("/a").toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(FAKE.bodies).hasSize(1);
        assertThat(FAKE.bodies.get(0)).contains("\"urlList\":[\"https://ex.com/a\"]");
    }

    @AfterAll
    static void stopFake() {
        FAKE.stop();
    }
}
