package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class LocaleFallbackTest {

    private static final String OLD_LANGUAGE = System.getProperty("user.language");
    private static final String OLD_COUNTRY = System.getProperty("user.country");

    static {
        System.setProperty("user.language", "fr");
        System.setProperty("user.country", "CA");
    }

    @AfterAll
    static void restore() {
        restore("user.language", OLD_LANGUAGE);
        restore("user.country", OLD_COUNTRY);
    }

    private static void restore(String key, String old) {
        if (old == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, old);
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class));

    @Test
    void buildMachineLocaleDoesNotLeak() {
        get("/t/locales").then().statusCode(200).body(is("[en-US]|en-US"));
    }
}
