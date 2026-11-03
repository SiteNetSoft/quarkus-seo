package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class AllowedHostsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class)
                    .addAsResource(new StringAsset("quarkus.seo.allowed-hosts=ex.com,www.ex.com\n"), "application.properties"));

    @Test
    void unknownHostFallsBackToTheFirstAllowedHost() {
        given().header("Host", "evil.example").get("/t/base").then().statusCode(200).body(is("http://ex.com"));
    }

    @Test
    void allowedHostIsKept() {
        given().header("Host", "www.ex.com").get("/t/base").then().statusCode(200).body(is("http://www.ex.com"));
    }
}
