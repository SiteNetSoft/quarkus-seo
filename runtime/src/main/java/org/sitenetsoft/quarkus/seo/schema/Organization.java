package org.sitenetsoft.quarkus.seo.schema;

import java.util.List;

/**
 * Builds a schema.org Organization node.
 */
public class Organization extends SchemaNode {

    private Organization() {
        super("Organization");
    }

    /**
     * Creates an empty node.
     *
     * @return the new node
     */
    public static Organization create() {
        return new Organization();
    }

    /**
     * Sets the organization name.
     *
     * @param name the name
     * @return this node
     */
    public Organization name(String name) {
        return set("name", name);
    }

    /**
     * Sets the organization URL.
     *
     * @param url the absolute URL
     * @return this node
     */
    public Organization url(String url) {
        return set("url", url);
    }

    /**
     * Sets the logo URL.
     *
     * @param logo the absolute URL of the logo
     * @return this node
     */
    public Organization logo(String logo) {
        return set("logo", logo);
    }

    /**
     * Sets the profile URLs of the organization.
     *
     * @param sameAs the absolute profile URLs
     * @return this node
     */
    public Organization sameAs(List<String> sameAs) {
        return set("sameAs", sameAs);
    }

    /**
     * Sets the node identifier.
     *
     * @param id the value of {@code @id}, or null to remove it
     * @return this node
     */
    @Override
    public Organization id(String id) {
        super.id(id);
        return this;
    }

    /**
     * Sets a property and replaces any earlier value.
     *
     * @param property the property name
     * @param value the new value, or null to remove the property
     * @return this node
     */
    @Override
    public Organization set(String property, Object value) {
        super.set(property, value);
        return this;
    }

    /**
     * Appends a value to a list property.
     *
     * @param property the property name
     * @param value the value to append
     * @return this node
     */
    @Override
    public Organization add(String property, Object value) {
        super.add(property, value);
        return this;
    }
}
