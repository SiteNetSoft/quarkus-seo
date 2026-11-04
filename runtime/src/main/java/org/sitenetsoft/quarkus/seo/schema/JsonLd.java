package org.sitenetsoft.quarkus.seo.schema;

import java.util.List;

/**
 * Writes a JSON-LD graph that is safe to place inside a script element.
 */
public final class JsonLd {

    private JsonLd() {
    }

    /**
     * Writes a schema.org graph.
     *
     * @param nodes the nodes to write
     * @param rawJson JSON objects to append verbatim after replacing each less-than sign
     * @return the JSON text, or an empty string when both lists are empty
     */
    public static String graph(List<SchemaNode> nodes, List<String> rawJson) {
        if (nodes.isEmpty() && rawJson.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("{\"@context\":\"https://schema.org\",\"@graph\":[");
        boolean first = true;
        for (SchemaNode node : nodes) {
            if (!first) {
                out.append(',');
            }
            first = false;
            node.writeTo(out);
        }
        for (String raw : rawJson) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append(raw.replace("<", "\\u003c"));
        }
        return out.append("]}").toString();
    }
}
