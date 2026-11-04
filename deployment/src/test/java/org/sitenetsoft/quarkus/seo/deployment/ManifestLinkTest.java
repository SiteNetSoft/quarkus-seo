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

class ManifestLinkTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.manifest.enabled", "true")
            .withApplicationRoot(jar -> jar.addClass(Pages.class)
                    .addAsResource(new StringAsset("<head>{#seo /}</head>"), "templates/page.html"));

    @Test
    void writesTheManifestLink() {
        String body = given().get("/page").then().statusCode(200).extract().asString();
        assertThat(body).contains("<link rel=\"manifest\" href=\"/manifest.webmanifest\">");
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
