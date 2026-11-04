package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class UnsafeHostTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(Res.class));

    @Test
    void serverRefusesAHostWithAPath() {
        given().header("Host", "evil.example/<x>").get("/t/base").then().statusCode(400);
    }

    @Test
    void normalHostIsKept() {
        String body = given().header("Host", "plain.example:8081").get("/t/base").then().statusCode(200)
                .extract().asString();
        assertThat(body).isEqualTo("http://plain.example:8081");
    }
}
