package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import org.sitenetsoft.quarkus.seo.Seo;

class SeoJavaRenderTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.title.template", "%s")
            .withApplicationRoot(jar -> jar.addClasses(PlainRoute.class));

    @Test
    void headRendersFromJava() {
        String body = given().get("/plain").then().statusCode(200).extract().asString();
        assertThat(body).startsWith("<title>Plain</title>");
    }

    public static class PlainRoute {

        @Inject
        Seo seo;

        @Route(path = "/plain")
        String plain() {
            seo.title("Plain");
            return seo.head();
        }
    }
}
