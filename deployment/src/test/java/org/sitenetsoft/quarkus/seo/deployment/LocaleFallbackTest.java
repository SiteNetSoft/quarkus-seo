package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class LocaleFallbackTest {

    static {
        System.setProperty("user.language", "fr");
        System.setProperty("user.country", "CA");
    }

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class));

    @Test
    void buildMachineLocaleDoesNotLeak() {
        get("/t/locales").then().statusCode(200).body(is("[en-US]|en-US"));
    }
}
