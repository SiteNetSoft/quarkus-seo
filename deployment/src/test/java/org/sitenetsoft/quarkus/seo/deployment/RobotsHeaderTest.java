package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.Seo;

class RobotsHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.title.template", "%s")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class)
                    .addAsResource(new StringAsset("pdf"), "META-INF/resources/file.pdf"));

    @Test
    void staticFileCarriesHeader() {
        get("/file.pdf").then().statusCode(200).header("X-Robots-Tag", is("noindex, nofollow"));
    }

    @Test
    void restMethodCarriesHeader() {
        get("/robots/plain").then().statusCode(200).header("X-Robots-Tag", is("noindex, nofollow"));
    }

    @Test
    void javaDirectivesStillSendHeader() {
        get("/robots/java").then().statusCode(200).header("X-Robots-Tag", is("noindex, nofollow"));
    }

    @Path("/robots")
    @Produces("text/plain")
    public static class Pages {

        @Inject
        Seo seo;

        @GET
        @Path("plain")
        public String plain() {
            return "ok";
        }

        @GET
        @Path("java")
        public String java() {
            seo.noindex().noimageindex();
            return "ok";
        }
    }
}
