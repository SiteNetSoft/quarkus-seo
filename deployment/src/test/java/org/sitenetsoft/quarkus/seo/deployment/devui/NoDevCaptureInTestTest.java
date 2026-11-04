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

import io.quarkus.arc.Arc;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.runtime.devui.SeoDevCapture;

class NoDevCaptureInTestTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLoggerName() != null
                    && record.getLoggerName().startsWith("org.sitenetsoft.quarkus.seo")
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records).isEmpty())
            .withApplicationRoot(jar -> jar.addClasses(Pages.class)
                    .addAsResource(new StringAsset("<head>{#seo title=\"x\" /}</head>"), "templates/page.html"));

    @Test
    void noCapture() {
        assertThat(Arc.container().instance(SeoDevCapture.class).isAvailable()).isFalse();
        given().get("/page").then().statusCode(200);
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
