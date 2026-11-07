package org.sitenetsoft.quarkus.seo.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusDevModeTest;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class PageCheckLogTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setLogRecordPredicate(record -> record.getLoggerName() != null
                    && record.getLoggerName().startsWith("org.sitenetsoft.quarkus.seo")
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .withApplicationRoot(jar -> jar.addClasses(Pages.class, Contributor.class)
                    .addAsResource(new StringAsset("<head>{#seo title=\"x\" /}</head>"), "templates/page.html")
                    .addAsResource(new StringAsset("quarkus.seo.sitemap.enabled=true\n"), "application.properties"));

    PageCheckLogTest() {
        super("quarkus-seo");
    }

    @Test
    void devUiChecksDoNotLogPageWarnings() throws Exception {
        executeJsonRPCMethod("checkPage", Map.of("path", "/page"));
        executeJsonRPCMethod("checkPage", Map.of("path", "/page"));
        executeJsonRPCMethod("checkSitemap");

        assertThat(TEST.getLogRecords()).noneMatch(record -> record.getMessage().startsWith("SEO check on /page"));
    }

    @Path("/")
    @Produces("text/html")
    public static class Pages {

        @Inject
        @Location("page.html")
        Template page;

        @GET
        @Path("page")
        public TemplateInstance page() {
            return page.instance();
        }
    }

    @Singleton
    public static class Contributor implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of(SitemapUrl.of("/page"));
        }
    }
}
