package org.sitenetsoft.quarkus.seo.schema;


/**
 * Builds a schema.org WebSite node.
 */
public class WebSite extends SchemaNode {

    private WebSite() {
        super("WebSite");
    }

    /**
     * Creates an empty node.
     *
     * @return the new node
     */
    public static WebSite create() {
        return new WebSite();
    }

    /**
     * Sets the site name.
     *
     * @param name the name
     * @return this node
     */
    public WebSite name(String name) {
        return set("name", name);
    }

    /**
     * Sets the site URL.
     *
     * @param url the absolute URL
     * @return this node
     */
    public WebSite url(String url) {
        return set("url", url);
    }

    /**
     * Sets the publisher and writes a reference when the publisher has an identifier.
     *
     * @param publisher the publisher node
     * @return this node
     */
    public WebSite publisher(SchemaNode publisher) {
        if (publisher == null) {
            return set("publisher", null);
        }
        if (publisher.id().isPresent()) {
            return set("publisher", SchemaNode.reference(publisher.id().get()));
        }
        return set("publisher", publisher);
    }

    /**
     * Sets the node identifier.
     *
     * @param id the value of {@code @id}, or null to remove it
     * @return this node
     */
    @Override
    public WebSite id(String id) {
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
    public WebSite set(String property, Object value) {
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
    public WebSite add(String property, Object value) {
        super.add(property, value);
        return this;
    }
}
