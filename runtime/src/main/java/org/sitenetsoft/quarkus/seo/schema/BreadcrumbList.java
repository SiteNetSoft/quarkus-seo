package org.sitenetsoft.quarkus.seo.schema;


/**
 * Builds a schema.org BreadcrumbList node.
 */
public class BreadcrumbList extends SchemaNode {

    private int position;

    private BreadcrumbList() {
        super("BreadcrumbList");
    }

    /**
     * Creates an empty node.
     *
     * @return the new node
     */
    public static BreadcrumbList create() {
        return new BreadcrumbList();
    }

    /**
     * Appends a breadcrumb with the next position, starting at 1.
     *
     * @param name the label of the breadcrumb
     * @param url the absolute URL of the breadcrumb
     * @return this node
     */
    public BreadcrumbList item(String name, String url) {
        position++;
        SchemaNode entry = SchemaNode.of("ListItem")
                .set("position", position)
                .set("name", name)
                .set("item", url);
        return add("itemListElement", entry);
    }

    /**
     * Sets the node identifier.
     *
     * @param id the value of {@code @id}, or null to remove it
     * @return this node
     */
    @Override
    public BreadcrumbList id(String id) {
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
    public BreadcrumbList set(String property, Object value) {
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
    public BreadcrumbList add(String property, Object value) {
        super.add(property, value);
        return this;
    }
}
