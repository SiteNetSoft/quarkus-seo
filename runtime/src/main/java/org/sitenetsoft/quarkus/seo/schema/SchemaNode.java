package org.sitenetsoft.quarkus.seo.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds one schema.org node with an ordered set of properties.
 */
public class SchemaNode {

    private final String type;
    private String id;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    /**
     * Creates a node of a schema.org type.
     *
     * @param type the schema.org type name
     */
    protected SchemaNode(String type) {
        this.type = type;
    }

    /**
     * Creates a node of a schema.org type.
     *
     * @param type the schema.org type name
     * @return the new node
     */
    public static SchemaNode of(String type) {
        return new SchemaNode(type);
    }

    /**
     * Creates a node that only references another node by identifier.
     *
     * @param id the identifier of the referenced node
     * @return a node that writes as an object with only {@code @id}
     */
    static SchemaNode reference(String id) {
        return new SchemaNode(null).id(id);
    }

    /**
     * Sets the node identifier.
     *
     * @param id the value of {@code @id}, or null to remove it
     * @return this node
     */
    public SchemaNode id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets a property and replaces any earlier value.
     *
     * @param property the property name
     * @param value a string, number, boolean, node, date, URI or list of these, or null to remove the property
     * @return this node
     */
    public SchemaNode set(String property, Object value) {
        if (value == null) {
            properties.remove(property);
        } else {
            properties.put(property, value);
        }
        return this;
    }

    /**
     * Appends a value to a list property.
     *
     * @param property the property name
     * @param value the value to append, where null changes nothing
     * @return this node
     */
    public SchemaNode add(String property, Object value) {
        if (value == null) {
            return this;
        }
        Object current = properties.get(property);
        List<Object> list = new ArrayList<>();
        if (current instanceof List<?> existing) {
            list.addAll(existing);
        } else if (current != null) {
            list.add(current);
        }
        if (value instanceof List<?> more) {
            list.addAll(more);
        } else {
            list.add(value);
        }
        properties.put(property, list);
        return this;
    }

    /**
     * Returns the schema.org type.
     *
     * @return the type name
     */
    public String type() {
        return type;
    }

    /**
     * Returns the node identifier.
     *
     * @return the identifier, or empty when none is set
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Appends this node as a JSON object.
     *
     * @param out the target
     */
    void writeTo(StringBuilder out) {
        out.append('{');
        boolean first = true;
        if (type != null) {
            out.append("\"@type\":");
            JsonWriter.string(out, type);
            first = false;
        }
        if (id != null) {
            if (!first) {
                out.append(',');
            }
            out.append("\"@id\":");
            JsonWriter.string(out, id);
            first = false;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!JsonWriter.writable(entry.getValue())) {
                continue;
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            JsonWriter.string(out, entry.getKey());
            out.append(':');
            JsonWriter.value(out, entry.getValue());
        }
        out.append('}');
    }
}
