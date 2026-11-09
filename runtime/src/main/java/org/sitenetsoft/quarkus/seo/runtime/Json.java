package org.sitenetsoft.quarkus.seo.runtime;

/**
 * Writes JSON strings for the generated files.
 *
 * <p>
 * This class is internal. Application code does not use it.
 */
public final class Json {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Json() {
    }

    /**
     * Appends a quoted and escaped JSON string.
     *
     * <p>
     * The method escapes the less-than sign, so the text stays safe inside a script element. It writes the same
     * text as the escape routine of the schema package.
     *
     * @param out the target
     * @param value the text to quote
     */
    public static void string(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c == '\u2028' || c == '\u2029' || c == '<') {
                        out.append("\\u").append(HEX[(c >> 12) & 0xF]).append(HEX[(c >> 8) & 0xF])
                                .append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
