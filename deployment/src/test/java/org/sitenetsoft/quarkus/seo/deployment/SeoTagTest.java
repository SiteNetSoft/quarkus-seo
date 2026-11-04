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
import org.sitenetsoft.quarkus.seo.Seo;

class SeoTagTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.title.template", "%s")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class, AppMessages.class)
                    .addAsResource(new StringAsset(
                            "<head>{#seo title=\"About\" description=\"Tag\" /}</head><h1>{seo:title}</h1>"),
                            "templates/about.html")
                    .addAsResource(new StringAsset("<head>{#seo title=msg:about_title /}</head>"),
                            "templates/bundle.html"));

    @Test
    void tagValuesRender() {
        String body = given().get("/about").then().statusCode(200).extract().asString();
        assertThat(body).contains("<title>About</title>").contains("<h1>About</h1>");
    }

    @Test
    void javaWinsOverTag() {
        String body = given().get("/about/java").then().statusCode(200).extract().asString();
        assertThat(body).contains("content=\"Java\"").doesNotContain("content=\"Tag\"");
    }

    @Test
    void messageBundleKeysWork() {
        String body = given().get("/about/bundle").then().statusCode(200).extract().asString();
        assertThat(body).contains("<title>Bundle about</title>");
    }

    @Test
    void canonicalUsesRequestPath() {
        String body = given().get("/about?utm=1").then().statusCode(200).extract().asString();
        assertThat(body).contains("<link rel=\"canonical\" href=\"http://localhost:").contains("/about\">");
    }

    @Path("/about")
    @Produces("text/html")
    public static class Pages {

        @Inject
        Seo seo;

        @Inject
        @Location("about.html")
        Template about;

        @Inject
        @Location("bundle.html")
        Template bundle;

        @GET
        public TemplateInstance page() {
            return about.instance();
        }

        @GET
        @Path("java")
        public TemplateInstance java() {
            seo.description("Java");
            return about.instance();
        }

        @GET
        @Path("bundle")
        public TemplateInstance bundle() {
            return bundle.instance();
        }
    }
}
