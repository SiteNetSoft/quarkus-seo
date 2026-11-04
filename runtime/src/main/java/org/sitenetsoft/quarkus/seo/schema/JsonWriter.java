package org.sitenetsoft.quarkus.seo.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Writes JSON text for schema values.
 *
 * <p>
 * This class is internal. Application code does not use it.
 */
public final class JsonWriter {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private JsonWriter() {
    }

    /**
     * Checks whether a value has a JSON form.
     *
     * @param value the value to test
     * @return false for null and for NaN or infinite numbers
     */
    static boolean writable(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Double d) {
            return !d.isNaN() && !d.isInfinite();
        }
        if (value instanceof Float f) {
            return !f.isNaN() && !f.isInfinite();
        }
        return true;
    }

    /**
     * Appends a value as JSON.
     *
     * @param out the target
     * @param value the value, which must be writable
     */
    static void value(StringBuilder out, Object value) {
        if (value instanceof String s) {
            string(out, s);
        } else if (value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long || value instanceof BigInteger
                || value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
            out.append(value.toString());
        } else if (value instanceof Number n) {
            out.append(n.toString());
        } else if (value instanceof SchemaNode node) {
            node.writeTo(out);
        } else if (value instanceof Instant || value instanceof OffsetDateTime || value instanceof LocalDate
                || value instanceof URI) {
            string(out, value.toString());
        } else if (value instanceof List<?> list) {
            list(out, list);
        } else {
            throw new IllegalArgumentException("Unsupported schema value type: " + value.getClass().getName());
        }
    }

    /**
     * Appends a list as a JSON array and skips the elements that have no JSON form.
     *
     * @param out the target
     * @param list the elements
     */
    static void list(StringBuilder out, List<?> list) {
        out.append('[');
        boolean first = true;
        for (Object element : list) {
            if (!writable(element)) {
                continue;
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            value(out, element);
        }
        out.append(']');
    }

    /**
     * Appends a quoted and escaped JSON string.
     *
     * <p>
     * The method escapes the less-than sign, so the text stays safe inside a script element.
     *
     * @param out the target
     * @param text the text to quote
     */
    public static void string(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ' || c == '<') {
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
