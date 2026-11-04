package org.sitenetsoft.quarkus.seo.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class JsonLdTest {

    @Test
    void writesGraphWithContextAndNodes() {
        String json = JsonLd.graph(List.of(
                Organization.create().id("https://ex.com/#organization").name("Ex").url("https://ex.com/")),
                List.of());
        assertThat(json).isEqualTo("{\"@context\":\"https://schema.org\",\"@graph\":[{\"@type\":\"Organization\","
                + "\"@id\":\"https://ex.com/#organization\",\"name\":\"Ex\",\"url\":\"https://ex.com/\"}]}");
    }

    @Test
    void escapesLessThanSoContentCannotCloseTheScript() {
        String json = JsonLd.graph(List.of(SchemaNode.of("Thing").set("name", "</script><b>")), List.of());
        assertThat(json).doesNotContain("<").contains("\\u003c/script>\\u003cb>");
    }

    @Test
    void escapesQuotesBackslashesControlAndLineSeparators() {
        String json = JsonLd.graph(List.of(SchemaNode.of("Thing").set("name", "a\"b\\c\nd\u2028e")), List.of());
        assertThat(json).contains("\"a\\\"b\\\\c\\nd\\u2028e\"");
    }

    @Test
    void embedsRawJsonVerbatimExceptLessThan() {
        String json = JsonLd.graph(List.of(), List.of("{\"@type\":\"Product\",\"name\":\"</x>\"}"));
        assertThat(json).endsWith("[{\"@type\":\"Product\",\"name\":\"\\u003c/x>\"}]}");
    }

    @Test
    void numbersBooleansDatesAndLists() {
        String json = JsonLd.graph(List.of(SchemaNode.of("Thing")
                .set("n", 3).set("b", true)
                .set("d", Instant.parse("2026-01-02T03:04:05Z"))
                .add("k", "x").add("k", "y")), List.of());
        assertThat(json).contains("\"n\":3,\"b\":true,\"d\":\"2026-01-02T03:04:05Z\",\"k\":[\"x\",\"y\"]");
    }

    @Test
    void breadcrumbPositionsStartAtOne() {
        String json = JsonLd.graph(List.of(BreadcrumbList.create().item("Home", "https://ex.com/")
                .item("Docs", "https://ex.com/docs")), List.of());
        assertThat(json).contains("\"position\":1,\"name\":\"Home\"").contains("\"position\":2,\"name\":\"Docs\"");
    }

    @Test
    void emptyInputGivesEmptyString() {
        assertThat(JsonLd.graph(List.of(), List.of())).isEmpty();
    }

    @Test
    void nullValueRemovesPropertyAndNonFiniteNumbersAreSkipped() {
        String json = JsonLd.graph(List.of(SchemaNode.of("Thing").set("a", "x").set("a", null)
                .set("f", Double.NaN).set("g", 1.5).set("i", Double.POSITIVE_INFINITY)), List.of());
        assertThat(json).contains("{\"@type\":\"Thing\",\"g\":1.5}");
    }

    @Test
    void publisherWithIdIsWrittenAsReference() {
        Organization org = Organization.create().id("https://ex.com/#organization").name("Ex");
        String json = JsonLd.graph(List.of(WebSite.create().name("S").publisher(org)), List.of());
        assertThat(json).contains("\"publisher\":{\"@id\":\"https://ex.com/#organization\"}");
    }

    @Test
    void publisherWithoutIdIsNested() {
        String json = JsonLd.graph(List.of(WebSite.create().publisher(Organization.create().name("Ex"))), List.of());
        assertThat(json).contains("\"publisher\":{\"@type\":\"Organization\",\"name\":\"Ex\"}");
    }

    @Test
    void articleWritesPersonAuthor() {
        String json = JsonLd.graph(List.of(Article.create().headline("H").author("Ann")
                .datePublished(Instant.parse("2026-01-02T03:04:05Z"))), List.of());
        assertThat(json).contains("\"headline\":\"H\"")
                .contains("\"author\":{\"@type\":\"Person\",\"name\":\"Ann\"}")
                .contains("\"datePublished\":\"2026-01-02T03:04:05Z\"");
    }
}
