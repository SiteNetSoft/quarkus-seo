package org.sitenetsoft.quarkus.seo.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

class AsyncTagParameterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.title.template", "%s")
            .withApplicationRoot(jar -> jar.addClasses(Pages.class)
                    .addAsResource(new StringAsset("<head>{#seo description=later /}</head>"),
                            "templates/later.html"));

    @Test
    void parameterCompletedOnAnotherThreadKeepsTheRequestState() {
        String body = given().get("/later").then().statusCode(200).extract().asString();
        assertThat(body).contains("<title>Java title</title>")
                .contains("<meta name=\"description\" content=\"Later\">")
                .contains("<link rel=\"canonical\" href=\"http://localhost:").contains("/later\">");
    }

    @Path("/later")
    @Produces("text/html")
    public static class Pages {

        @Inject
        Seo seo;

        @Inject
        @Location("later.html")
        Template later;

        @GET
        public TemplateInstance page() {
            seo.title("Java title");
            CompletableFuture<String> value = CompletableFuture.supplyAsync(() -> "Later",
                    CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS));
            return later.data("later", value);
        }
    }
}
