package org.sitenetsoft.quarkus.seo.runtime;

/**
 * Escape text for HTML content and attribute values.
 */
final class Html {

    private Html() {
    }

    /**
     * Escape the characters that are special in HTML.
     *
     * @param text the text to escape
     * @return the escaped text
     */
    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
