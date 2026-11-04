package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

class ManifestLinkRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.manifest.enabled", "true")
            .overrideConfigKey("quarkus.seo.manifest.path", "/site.webmanifest")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .withApplicationRoot(jar -> jar.addClass(Pages.class)
                    .addAsResource(new StringAsset("<head>{#seo /}</head>"), "templates/page.html"));

    @Test
    void writesTheLinkUnderTheRootPath() {
        String body = given().basePath("/app").get("/page").then().statusCode(200).extract().asString();
        assertThat(body).contains("<link rel=\"manifest\" href=\"/app/site.webmanifest\">");
    }

    @Test
    void servesTheManifestUnderTheRootPath() {
        given().basePath("/app").get("/site.webmanifest").then().statusCode(200)
                .header("Content-Type", "application/manifest+json");
    }

    @Path("/page")
    @Produces("text/html")
    public static class Pages {

        @Inject
        @Location("page.html")
        Template page;

        @GET
        public TemplateInstance page() {
            return page.instance();
        }
    }
}
