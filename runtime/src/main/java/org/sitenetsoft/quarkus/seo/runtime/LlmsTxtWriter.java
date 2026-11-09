package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.Optional;

import org.sitenetsoft.quarkus.seo.SitemapUrl;

/**
 * Write the llms.txt Markdown file.
 */
final class LlmsTxtWriter {

    private LlmsTxtWriter() {
    }

    /**
     * Write the file: a heading, an optional quote and one link per titled URL.
     *
     * <p>
     * A URL with no title or a blank title has no link text, so the writer leaves it out.
     *
     * @param heading the site name or base URL
     * @param description the site description
     * @param urls the URLs, in order
     * @param baseUrl the base URL that makes paths absolute
     * @return the Markdown text, which ends with a newline
     */
    static String write(String heading, Optional<String> description, List<SitemapUrl> urls, String baseUrl) {
        StringBuilder out = new StringBuilder("# ").append(oneLine(heading)).append('\n');
        String quote = description.map(LlmsTxtWriter::oneLine).orElse("");
        if (!quote.isEmpty()) {
            out.append("\n> ").append(quote).append('\n');
        }
        boolean first = true;
        for (SitemapUrl url : urls) {
            String title = url.title().map(LlmsTxtWriter::oneLine).orElse("");
            if (title.isEmpty()) {
                continue;
            }
            if (first) {
                out.append('\n');
                first = false;
            }
            out.append("- [").append(escapeTitle(title)).append("](")
                    .append(encodeUrl(Urls.absolute(baseUrl, url.loc()))).append(")\n");
        }
        return out.toString();
    }

    /**
     * Turn each run of line breaks into one space and trim the text.
     */
    private static String oneLine(String text) {
        return text.replaceAll("[\\r\\n]+", " ").trim();
    }

    /**
     * Escape the characters that end or open a Markdown link.
     */
    private static String escapeTitle(String title) {
        StringBuilder out = new StringBuilder(title.length());
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            switch (c) {
                case '[', ']', '\\' -> out.append('\\').append(c);
                case '<' -> out.append("&lt;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Percent-encode the characters that end a Markdown link target or break the line.
     */
    private static String encodeUrl(String url) {
        StringBuilder out = new StringBuilder(url.length());
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c <= ' ' || c == '(' || c == ')' || c == '<' || c == '>' || c == '\\' || c == 0x7f) {
                out.append('%').append(String.format("%02X", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
