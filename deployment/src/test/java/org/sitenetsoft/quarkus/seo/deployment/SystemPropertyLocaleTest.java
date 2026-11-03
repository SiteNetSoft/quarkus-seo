package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SystemPropertyLocaleTest {

    private static final String OLD_LANGUAGE = System.getProperty("user.language");
    private static final String OLD_COUNTRY = System.getProperty("user.country");

    static {
        // The explicit value equals the machine default on purpose.
        System.setProperty("user.language", "fr");
        System.setProperty("user.country", "CA");
        System.setProperty("quarkus.locales", "fr-CA");
    }

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class));

    @AfterAll
    static void restore() {
        System.clearProperty("quarkus.locales");
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

    @Test
    void systemPropertyEqualToTheMachineDefaultStillCounts() {
        get("/t/locales").then().statusCode(200).body(is("[fr-CA]|fr-CA"));
    }
}
