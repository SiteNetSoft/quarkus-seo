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
                    .addAsResource(new StringAsset("quarkus.seo.allowed-hosts=ex.com,www.ex.com,api.ex.com:8443\n"), "application.properties"));

    @Test
    void unknownHostFallsBackToTheFirstAllowedHost() {
        given().header("Host", "evil.example").get("/t/base").then().statusCode(200).body(is("http://ex.com"));
    }

    @Test
    void caseVariantGivesTheAllowedEntry() {
        given().header("Host", "EX.com").get("/t/base").then().statusCode(200).body(is("http://ex.com"));
    }

    @Test
    void portVariantGivesTheAllowedEntryWithoutAPort() {
        given().header("Host", "ex.com:6666").get("/t/base").then().statusCode(200).body(is("http://ex.com"));
    }

    @Test
    void entryWithAPortKeepsItsPort() {
        given().header("Host", "api.ex.com:9999").get("/t/base").then().statusCode(200)
                .body(is("http://api.ex.com:8443"));
        given().header("Host", "api.ex.com:8443").get("/t/base").then().statusCode(200)
                .body(is("http://api.ex.com:8443"));
    }

    @Test
    void allowedHostIsKept() {
        given().header("Host", "www.ex.com").get("/t/base").then().statusCode(200).body(is("http://www.ex.com"));
    }
}
