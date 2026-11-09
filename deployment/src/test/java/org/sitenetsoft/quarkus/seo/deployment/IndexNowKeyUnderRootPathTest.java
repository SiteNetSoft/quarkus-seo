package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class IndexNowKeyUnderRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .overrideConfigKey("quarkus.seo.indexnow.key", "abcdef0123456789")
            .overrideConfigKey("quarkus.seo.indexnow.serve-under-root-path", "true");

    @Test
    void keyFileMovesUnderTheRootPath() {
        String body = given().basePath("").get("/app/abcdef0123456789.txt").then().statusCode(200).extract()
                .asString();
        assertThat(body).isEqualTo("abcdef0123456789");
        given().basePath("").get("/abcdef0123456789.txt").then().statusCode(404);
    }
}
