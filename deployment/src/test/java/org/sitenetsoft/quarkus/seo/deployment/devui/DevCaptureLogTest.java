package org.sitenetsoft.quarkus.seo.deployment.devui;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

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
import io.quarkus.test.QuarkusDevModeTest;

class DevCaptureLogTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setLogRecordPredicate(record -> record.getLoggerName() != null
                    && record.getLoggerName().startsWith("org.sitenetsoft.quarkus.seo")
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .withApplicationRoot(jar -> jar.addClasses(Pages.class)
                    .addAsResource(new StringAsset("<head>{#seo title=\"x\" /}</head>"), "templates/page.html"));

    @Test
    void warningLogsOncePerPath() {
        given().get("/page").then().statusCode(200);
        given().get("/page").then().statusCode(200);
        assertThat(count("SEO check on /page: ", "[missing-description]")).isEqualTo(1);

        TEST.modifyResourceFile("templates/page.html", text -> text.replace("title=\"x\"",
                "title=\"x\" description=\"" + "d".repeat(100) + "\""));
        given().get("/page").then().statusCode(200);
        assertThat(count("SEO check on /page: ", "[missing-description]")).isEqualTo(1);
    }

    private static long count(String prefix, String code) {
        return TEST.getLogRecords().stream()
                .filter(record -> record.getMessage().startsWith(prefix) && record.getMessage().endsWith(code))
                .count();
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
