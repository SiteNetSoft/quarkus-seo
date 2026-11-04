package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;
import org.sitenetsoft.quarkus.seo.Seo;

class MasterSwitchTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.enabled", "false")
            .withApplicationRoot(jar -> jar.addClasses(Page.class));

    @Test
    void servesNoSitemap() {
        given().basePath("").get("/sitemap.xml").then().statusCode(404);
    }

    @Test
    void servesNoRobotsTxt() {
        given().basePath("").get("/robots.txt").then().statusCode(404);
    }

    @Test
    void sendsNoRobotsHeader() {
        Response response = given().get("/master/page");
        response.then().statusCode(200);
        assertThat(response.header("X-Robots-Tag")).isNull();
    }

    @Test
    void registersNoSeoBean() {
        assertThat(Arc.container().instance(Seo.class).isAvailable()).isFalse();
    }

    @Path("/master")
    @Produces("text/plain")
    public static class Page {

        @GET
        @Path("page")
        public String page() {
            return "ok";
        }
    }
}
