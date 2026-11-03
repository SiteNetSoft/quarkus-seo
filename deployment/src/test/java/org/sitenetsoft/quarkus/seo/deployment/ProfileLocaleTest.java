package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ProfileLocaleTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class)
                    .addAsResource(new StringAsset("""
                            %test.quarkus.locales=de,en
                            %prod.quarkus.locales=fr
                            """), "application.properties"));

    @Test
    void profilePrefixedKeyOfTheActiveProfileIsUsed() {
        get("/t/locales").then().statusCode(200).body(is("[de, en]|de"));
    }
}
