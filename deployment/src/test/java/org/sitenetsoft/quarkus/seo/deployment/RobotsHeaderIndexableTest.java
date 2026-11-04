package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

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

class RobotsHeaderIndexableTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.title.template", "%s")
            .overrideConfigKey("quarkus.seo.indexable", "true")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class)
                    .addAsResource(new StringAsset("<head>{#seo title=\"T\" noindex=true /}</head>"),
                            "templates/tagonly.html"));

    @Test
    void plainMethodHasNoHeader() {
        assertThat(get("/robots/plain").then().statusCode(200).extract().header("X-Robots-Tag")).isNull();
    }

    @Test
    void javaDirectivesSendHeader() {
        get("/robots/java").then().statusCode(200).header("X-Robots-Tag", is("noindex, noimageindex"));
    }

    @Test
    void tagOnlyDirectivesStayInMeta() {
        var response = get("/robots/tag").then().statusCode(200).extract();
        assertThat(response.header("X-Robots-Tag")).isNull();
        assertThat(response.asString()).contains("name=\"robots\"").contains("noindex");
    }

    @Path("/robots")
    @Produces("text/html")
    public static class Pages {

        @Inject
        Seo seo;

        @Inject
        @Location("tagonly.html")
        Template tagonly;

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

        @GET
        @Path("tag")
        public TemplateInstance tag() {
            return tagonly.instance();
        }
    }
}
