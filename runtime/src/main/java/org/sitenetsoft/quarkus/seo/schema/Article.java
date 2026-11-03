package org.sitenetsoft.quarkus.seo.schema;

import java.time.Instant;

/**
 * Builds a schema.org Article node.
 */
public class Article extends SchemaNode {

    private Article() {
        super("Article");
    }

    /**
     * Creates an empty node.
     *
     * @return the new node
     */
    public static Article create() {
        return new Article();
    }

    /**
     * Sets the headline.
     *
     * @param headline the headline
     * @return this node
     */
    public Article headline(String headline) {
        return set("headline", headline);
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this node
     */
    public Article description(String description) {
        return set("description", description);
    }

    /**
     * Sets the image.
     *
     * @param image the absolute URL of the image
     * @return this node
     */
    public Article image(String image) {
        return set("image", image);
    }

    /**
     * Sets the publication date.
     *
     * @param datePublished the instant of publication
     * @return this node
     */
    public Article datePublished(Instant datePublished) {
        return set("datePublished", datePublished);
    }

    /**
     * Sets the modification date.
     *
     * @param dateModified the instant of the last change
     * @return this node
     */
    public Article dateModified(Instant dateModified) {
        return set("dateModified", dateModified);
    }

    /**
     * Sets the author as a Person node.
     *
     * @param name the name of the author
     * @return this node
     */
    public Article author(String name) {
        if (name == null) {
            return set("author", null);
        }
        return set("author", SchemaNode.of("Person").set("name", name));
    }

    /**
     * Sets the author.
     *
     * @param author the author node
     * @return this node
     */
    public Article author(SchemaNode author) {
        return set("author", author);
    }

    /**
     * Sets the node identifier.
     *
     * @param id the value of {@code @id}, or null to remove it
     * @return this node
     */
    @Override
    public Article id(String id) {
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
    public Article set(String property, Object value) {
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
    public Article add(String property, Object value) {
        super.add(property, value);
        return this;
    }
}
