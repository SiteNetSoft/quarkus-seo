package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ConfigFallbackTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class)
                    .addAsResource(new StringAsset("""
                            quarkus.application.name=Acme
                            quarkus.locales=fr,en
                            """), "application.properties"));

    @Test
    void siteNameFallsBackToTheApplicationName() {
        get("/t/name").then().statusCode(200).body(is("Acme"));
    }

    @Test
    void quarkusLocalesAreUsed() {
        get("/t/locales").then().statusCode(200).body(is("[fr, en]|fr"));
    }
}
