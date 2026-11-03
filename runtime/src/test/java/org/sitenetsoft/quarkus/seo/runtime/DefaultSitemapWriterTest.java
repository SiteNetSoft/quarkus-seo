package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.sitenetsoft.quarkus.seo.ChangeFrequency;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class DefaultSitemapWriterTest {

    private final DefaultSitemapWriter writer = new DefaultSitemapWriter();

    private static void validate(String xsd, String xml) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        Schema schema = factory.newSchema(new StreamSource(
                DefaultSitemapWriterTest.class.getResourceAsStream("/xsd/" + xsd)));
        schema.newValidator().validate(new DOMSource(withoutExtensions(xml)));
    }

    // The sitemaps.org schema validates foreign elements strictly, so the xhtml and image
    // elements are removed first. Other tests check those elements as text.
    private static Document withoutExtensions(String xml) throws Exception {
        DocumentBuilderFactory builders = DocumentBuilderFactory.newInstance();
        builders.setNamespaceAware(true);
        Document document = builders.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        NodeList all = document.getElementsByTagName("*");
        List<Node> foreign = new ArrayList<>();
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (!"http://www.sitemaps.org/schemas/sitemap/0.9".equals(node.getNamespaceURI())) {
                foreign.add(node);
            }
        }
        for (Node node : foreign) {
            node.getParentNode().removeChild(node);
        }
        return document;
    }

    @Test
    void urlSetIsValidAgainstTheSchema() throws Exception {
        SitemapUrl full = SitemapUrl.of("/fr/a").lastmod(Instant.parse("2026-01-02T03:04:05Z"))
                .changefreq(ChangeFrequency.WEEKLY).priority(1).alternate(Locale.FRENCH, "/fr/a")
                .alternate(Locale.ENGLISH, "/en/a").image("/i.png").title("A");
        String xml = writer.urlSet(List.of(full, SitemapUrl.of("/b")), "https://ex.com", Locale.ENGLISH);
        validate("sitemap.xsd", xml);
    }

    @Test
    void indexIsValidAgainstTheSchema() throws Exception {
        String xml = writer.index(List.of("https://ex.com/sitemap-1.xml", "https://ex.com/sitemap-2.xml"));
        assertThat(xml).contains("<sitemapindex", "<loc>https://ex.com/sitemap-2.xml</loc>");
        validate("siteindex.xsd", xml);
    }

    @Test
    void locIsAbsoluteEscapedAndAscii() {
        String xml = writer.urlSet(List.of(SitemapUrl.of("/café?a=1&b=2")), "https://ex.com", Locale.ENGLISH);
        assertThat(xml).contains("<loc>https://ex.com/caf%C3%A9?a=1&amp;b=2</loc>");
    }

    @Test
    void unparsableUrlsArePercentEncoded() {
        String xml = writer.urlSet(List.of(SitemapUrl.of("/a é")), "https://ex.com", Locale.ENGLISH);
        assertThat(xml).contains("<loc>https://ex.com/a%20%C3%A9</loc>");
    }

    @Test
    void optionalFieldsLeftOutUnlessSet() {
        String xml = writer.urlSet(List.of(SitemapUrl.of("/a")), "https://ex.com", Locale.ENGLISH);
        assertThat(xml).doesNotContain("<lastmod>", "<changefreq>", "<priority>");
    }

    @Test
    void lastmodChangefreqPriority() {
        String xml = writer.urlSet(List.of(SitemapUrl.of("/a").lastmod(Instant.parse("2026-01-02T03:04:05Z"))
                .changefreq(ChangeFrequency.WEEKLY).priority(0.8)), "https://ex.com", Locale.ENGLISH);
        assertThat(xml).contains("<lastmod>2026-01-02T03:04:05Z</lastmod><changefreq>weekly</changefreq>"
                + "<priority>0.8</priority>");
    }

    @Test
    void priorityHasOneDecimal() {
        String xml = writer.urlSet(List.of(SitemapUrl.of("/a").priority(1)), "https://ex.com", Locale.ENGLISH);
        assertThat(xml).contains("<priority>1.0</priority>");
    }

    @Test
    void priorityOutsideTheRangeIsRejected() {
        assertThatThrownBy(() -> SitemapUrl.of("/a").priority(1.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SitemapUrl.of("/a").priority(-0.1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alternatesWriteSelfOthersAndXDefault() {
        String xml = writer.urlSet(List.of(SitemapUrl.of("/fr/a").alternate(Locale.FRENCH, "/fr/a")
                .alternate(Locale.ENGLISH, "/en/a")), "https://ex.com", Locale.ENGLISH);
        assertThat(xml).contains("xmlns:xhtml=\"http://www.w3.org/1999/xhtml\"",
                "<xhtml:link rel=\"alternate\" hreflang=\"fr\" href=\"https://ex.com/fr/a\"/>",
                "<xhtml:link rel=\"alternate\" hreflang=\"en\" href=\"https://ex.com/en/a\"/>",
                "<xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"https://ex.com/en/a\"/>");
    }

    @Test
    void imagesUseTheImageNamespace() {
        String xml = writer.urlSet(List.of(SitemapUrl.of("/a").image("/i.png")), "https://ex.com", Locale.ENGLISH);
        assertThat(xml).contains("xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\"",
                "<image:image><image:loc>https://ex.com/i.png</image:loc></image:image>");
    }
}
