package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SiteUrlTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class)
                    .addAsResource(new StringAsset("quarkus.seo.site-url=https://ex.com/\n"), "application.properties"));

    @Test
    void siteUrlWinsOverTheHost() {
        given().header("Host", "evil.example").get("/t/base").then().statusCode(200).body(is("https://ex.com"));
    }
}
