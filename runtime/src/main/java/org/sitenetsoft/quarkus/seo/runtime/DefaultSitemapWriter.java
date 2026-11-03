package org.sitenetsoft.quarkus.seo.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import org.sitenetsoft.quarkus.seo.SitemapUrl;
import org.sitenetsoft.quarkus.seo.SitemapWriter;

/**
 * Write sitemaps and sitemap indexes as XML text.
 */
@DefaultBean
@Singleton
public class DefaultSitemapWriter implements SitemapWriter {

    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String SITEMAP_NS = "http://www.sitemaps.org/schemas/sitemap/0.9";

    @Override
    public String urlSet(List<SitemapUrl> urls, String baseUrl, Locale defaultLocale) {
        StringBuilder xml = new StringBuilder(HEADER);
        xml.append("<urlset xmlns=\"").append(SITEMAP_NS).append("\"")
                .append(" xmlns:xhtml=\"http://www.w3.org/1999/xhtml\"")
                .append(" xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");
        for (SitemapUrl url : urls) {
            writeUrl(xml, url, baseUrl, defaultLocale);
        }
        return xml.append("</urlset>\n").toString();
    }

    @Override
    public String index(List<String> pageUrls) {
        StringBuilder xml = new StringBuilder(HEADER);
        xml.append("<sitemapindex xmlns=\"").append(SITEMAP_NS).append("\">\n");
        for (String pageUrl : pageUrls) {
            xml.append("<sitemap><loc>").append(escape(ascii(pageUrl))).append("</loc></sitemap>\n");
        }
        return xml.append("</sitemapindex>\n").toString();
    }

    private static void writeUrl(StringBuilder xml, SitemapUrl url, String baseUrl, Locale defaultLocale) {
        xml.append("<url><loc>").append(location(baseUrl, url.loc())).append("</loc>");
        if (url.lastmod().isPresent()) {
            xml.append("<lastmod>").append(DateTimeFormatter.ISO_INSTANT.format(url.lastmod().get()))
                    .append("</lastmod>");
        }
        if (url.changefreq().isPresent()) {
            xml.append("<changefreq>").append(url.changefreq().get().name().toLowerCase(Locale.ROOT))
                    .append("</changefreq>");
        }
        if (url.priority().isPresent()) {
            xml.append("<priority>").append(String.format(Locale.ROOT, "%.1f", url.priority().getAsDouble()))
                    .append("</priority>");
        }
        writeAlternates(xml, url, baseUrl, defaultLocale);
        for (String image : url.images()) {
            xml.append("<image:image><image:loc>").append(location(baseUrl, image))
                    .append("</image:loc></image:image>");
        }
        xml.append("</url>\n");
    }

    private static void writeAlternates(StringBuilder xml, SitemapUrl url, String baseUrl, Locale defaultLocale) {
        String xDefault = null;
        for (Map.Entry<Locale, String> alternate : url.alternates().entrySet()) {
            String href = location(baseUrl, alternate.getValue());
            writeLink(xml, alternate.getKey().toLanguageTag(), href);
            if (defaultLocale != null && alternate.getKey().equals(defaultLocale)) {
                xDefault = href;
            }
        }
        if (xDefault == null && defaultLocale != null) {
            for (Map.Entry<Locale, String> alternate : url.alternates().entrySet()) {
                if (alternate.getKey().getLanguage().equals(defaultLocale.getLanguage())) {
                    xDefault = location(baseUrl, alternate.getValue());
                    break;
                }
            }
        }
        if (xDefault != null) {
            writeLink(xml, "x-default", xDefault);
        }
    }

    private static void writeLink(StringBuilder xml, String hreflang, String href) {
        xml.append("<xhtml:link rel=\"alternate\" hreflang=\"").append(escape(hreflang))
                .append("\" href=\"").append(href).append("\"/>");
    }

    private static String location(String baseUrl, String pathOrUrl) {
        return escape(ascii(Urls.absolute(baseUrl, pathOrUrl)));
    }

    private static String ascii(String url) {
        try {
            return new URI(url).toASCIIString();
        } catch (URISyntaxException e) {
            StringBuilder encoded = new StringBuilder();
            for (byte b : url.getBytes(StandardCharsets.UTF_8)) {
                int value = b & 0xFF;
                if (value > 0x20 && value < 0x7F && value != '"' && value != '<' && value != '>') {
                    encoded.append((char) value);
                } else {
                    encoded.append('%').append(String.format(Locale.ROOT, "%02X", value));
                }
            }
            return encoded.toString();
        }
    }

    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
