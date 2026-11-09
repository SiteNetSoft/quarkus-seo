package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ConfigOverrideTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class)
                    .addAsResource(new StringAsset("""
                            quarkus.application.name=Acme
                            quarkus.seo.site-name=Shop
                            quarkus.locales=de
                            quarkus.seo.locales=fr,en
                            quarkus.seo.default-locale=en
                            """), "application.properties"));

    @Test
    void seoKeysWin() {
        get("/t/name").then().statusCode(200).body(is("Shop"));
        get("/t/locales").then().statusCode(200).body(is("[fr, en]|en"));
    }
}
