package org.sitenetsoft.quarkus.seo.deployment.devui;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusDevModeTest;
import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class SeoDevUiJsonRpcTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClasses(Pages.class, Contributor.class)
                    .addAsResource(new StringAsset("<head>{#seo title=\"Hello\" /}</head>"), "templates/page.html")
                    .addAsResource(new StringAsset("quarkus.seo.sitemap.enabled=true\n"), "application.properties"));

    SeoDevUiJsonRpcTest() {
        super("quarkus-seo");
    }

    @Test
    void checkPageReportsTheHead() throws Exception {
        JsonNode result = executeJsonRPCMethod("checkPage", Map.of("path", "/page"));

        assertThat(result.get("status").asInt()).isEqualTo(200);
        assertThat(result.get("head").asText()).contains("<title>Hello");
        assertThat(codes(result.get("warnings"))).contains("missing-description");
        assertThat(result.get("snippet").get("title").asText()).startsWith("Hello");
        assertThat(result.get("snippet").get("url").asText()).endsWith("/page");
        assertThat(result.get("card").get("title").asText()).startsWith("Hello");
    }

    @Test
    void checkPageRefusesOtherHosts() throws Exception {
        for (String path : List.of("https://evil.example/", "//evil.example/x", "x")) {
            JsonNode result = executeJsonRPCMethod("checkPage", Map.of("path", path));

            assertThat(result.has("error")).as(path).isTrue();
            assertThat(result.has("status")).as(path).isFalse();
        }
    }

    @Test
    void checkSitemapFindsDuplicateTitles() throws Exception {
        JsonNode result = executeJsonRPCMethod("checkSitemap");

        assertThat(result.get("checked").asInt()).isEqualTo(2);
        assertThat(result.get("skipped").asInt()).isZero();
        List<JsonNode> duplicates = new ArrayList<>();
        for (JsonNode warning : result.get("warnings")) {
            if ("duplicate-title".equals(warning.path("code").asText())) {
                duplicates.add(warning);
            }
        }
        assertThat(duplicates).hasSize(1);
        assertThat(duplicates.get(0).get("message").asText()).contains("/page,").contains("/page2");
    }

    @Test
    void overviewShowsTheSite() throws Exception {
        JsonNode result = executeJsonRPCMethod("getOverview");

        assertThat(result.get("indexable").asBoolean()).isFalse();
        assertThat(result.get("siteUrl").asText()).isEqualTo("request host");
        assertThat(result.get("defaultLocale").asText()).isNotBlank();
    }

    @Test
    void devUiServesTheComponentsAndTheOutputs() {
        given().get("/q/dev-ui/quarkus-seo/qwc-seo-overview.js").then().statusCode(200)
                .body(containsString("qwc-seo-overview"));
        given().get("/q/dev-ui/quarkus-seo/qwc-seo-page-check.js").then().statusCode(200)
                .body(containsString("qwc-seo-page-check"));
        given().get("/q/dev-ui/quarkus-seo-data.js").then().statusCode(200)
                .body(containsString("\"name\":\"sitemap\""))
                .body(containsString("\"servedBy\":\"extension\""))
                .body(containsString("\"servedBy\":\"off\""));
    }

    private static List<String> codes(JsonNode warnings) {
        List<String> codes = new ArrayList<>();
        for (JsonNode warning : warnings) {
            codes.add(warning.get("code").asText());
        }
        return codes;
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

        @GET
        @Path("page2")
        public TemplateInstance page2() {
            return page.instance();
        }
    }

    @Singleton
    public static class Contributor implements SitemapContributor {

        @Override
        public Stream<SitemapUrl> urls() {
            return Stream.of(SitemapUrl.of("/page"), SitemapUrl.of("/page2"));
        }
    }
}
