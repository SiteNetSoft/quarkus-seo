package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SecurityTxtConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.security-txt.enabled", "true")
            .overrideConfigKey("quarkus.seo.security-txt.contact", "mailto:sec@ex.com")
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset("Contact: mine\n"),
                    "META-INF/resources/.well-known/security.txt"));

    @Test
    void servesTheApplicationFile() {
        String body = given().basePath("").get("/.well-known/security.txt").then().statusCode(200).extract()
                .asString();
        assertThat(body).isEqualTo("Contact: mine\n");
    }
}
