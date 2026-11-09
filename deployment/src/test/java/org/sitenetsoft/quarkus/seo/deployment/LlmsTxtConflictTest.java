package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class LlmsTxtConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.llms-txt.enabled", "true")
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset("mine"), "META-INF/resources/llms.txt"));

    @Test
    void servesTheApplicationFile() {
        String body = given().basePath("").get("/llms.txt").then().statusCode(200).extract().asString();
        assertThat(body).isEqualTo("mine");
    }
}
