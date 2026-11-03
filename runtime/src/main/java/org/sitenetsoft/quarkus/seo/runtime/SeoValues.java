package org.sitenetsoft.quarkus.seo.runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.schema.SchemaNode;

/**
 * Hold the page values of one layer, where each field is either set or not set.
 *
 * <p>
 * A blank string counts as not set when it is stored.
 */
public class SeoValues {

    private String title;
    private String absoluteTitle;
    private String description;
    private String canonical;
    private String type;
    private String image;
    private Integer imageWidth;
    private Integer imageHeight;
    private String imageAlt;
    private Locale locale;
    private Instant published;
    private Instant modified;
    private String author;
    private String section;
    private String twitterCard;
    private Integer maxSnippet;
    private String maxImagePreview;
    private Instant unavailableAfter;
    private Boolean noindex;
    private Boolean nofollow;
    private Boolean noimageindex;
    private final LinkedHashMap<Locale, String> alternates = new LinkedHashMap<>();
    private final List<String> tags = new ArrayList<>();
    private final List<SchemaNode> jsonLdNodes = new ArrayList<>();
    private final List<String> jsonLdRaw = new ArrayList<>();
    private final List<ResolvedSeo.Meta> extraMeta = new ArrayList<>();

    /**
     * Set the page title.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues title(String value) {
        this.title = blank(value);
        return this;
    }

    /**
     * Return the page title.
     *
     * @return the value, or null when it is not set
     */
    public String title() {
        return title;
    }

    /**
     * Set the title that skips the template.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues absoluteTitle(String value) {
        this.absoluteTitle = blank(value);
        return this;
    }

    /**
     * Return the title that skips the template.
     *
     * @return the value, or null when it is not set
     */
    public String absoluteTitle() {
        return absoluteTitle;
    }

    /**
     * Set the description.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues description(String value) {
        this.description = blank(value);
        return this;
    }

    /**
     * Return the description.
     *
     * @return the value, or null when it is not set
     */
    public String description() {
        return description;
    }

    /**
     * Set the canonical URL or path.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues canonical(String value) {
        this.canonical = blank(value);
        return this;
    }

    /**
     * Return the canonical URL or path.
     *
     * @return the value, or null when it is not set
     */
    public String canonical() {
        return canonical;
    }

    /**
     * Set the Open Graph type.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues type(String value) {
        this.type = blank(value);
        return this;
    }

    /**
     * Return the Open Graph type.
     *
     * @return the value, or null when it is not set
     */
    public String type() {
        return type;
    }

    /**
     * Set the image URL or path.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues image(String value) {
        this.image = blank(value);
        return this;
    }

    /**
     * Return the image URL or path.
     *
     * @return the value, or null when it is not set
     */
    public String image() {
        return image;
    }

    /**
     * Set the image alt text.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues imageAlt(String value) {
        this.imageAlt = blank(value);
        return this;
    }

    /**
     * Return the image alt text.
     *
     * @return the value, or null when it is not set
     */
    public String imageAlt() {
        return imageAlt;
    }

    /**
     * Set the article author.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues author(String value) {
        this.author = blank(value);
        return this;
    }

    /**
     * Return the article author.
     *
     * @return the value, or null when it is not set
     */
    public String author() {
        return author;
    }

    /**
     * Set the article section.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues section(String value) {
        this.section = blank(value);
        return this;
    }

    /**
     * Return the article section.
     *
     * @return the value, or null when it is not set
     */
    public String section() {
        return section;
    }

    /**
     * Set the Twitter card type.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues twitterCard(String value) {
        this.twitterCard = blank(value);
        return this;
    }

    /**
     * Return the Twitter card type.
     *
     * @return the value, or null when it is not set
     */
    public String twitterCard() {
        return twitterCard;
    }

    /**
     * Set the max-image-preview directive value.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues maxImagePreview(String value) {
        this.maxImagePreview = blank(value);
        return this;
    }

    /**
     * Return the max-image-preview directive value.
     *
     * @return the value, or null when it is not set
     */
    public String maxImagePreview() {
        return maxImagePreview;
    }

    /**
     * Set the image width.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues imageWidth(Integer value) {
        this.imageWidth = value;
        return this;
    }

    /**
     * Return the image width.
     *
     * @return the value, or null when it is not set
     */
    public Integer imageWidth() {
        return imageWidth;
    }

    /**
     * Set the image height.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues imageHeight(Integer value) {
        this.imageHeight = value;
        return this;
    }

    /**
     * Return the image height.
     *
     * @return the value, or null when it is not set
     */
    public Integer imageHeight() {
        return imageHeight;
    }

    /**
     * Set the page locale.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues locale(Locale value) {
        this.locale = value;
        return this;
    }

    /**
     * Return the page locale.
     *
     * @return the value, or null when it is not set
     */
    public Locale locale() {
        return locale;
    }

    /**
     * Set the publication time.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues published(Instant value) {
        this.published = value;
        return this;
    }

    /**
     * Return the publication time.
     *
     * @return the value, or null when it is not set
     */
    public Instant published() {
        return published;
    }

    /**
     * Set the modification time.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues modified(Instant value) {
        this.modified = value;
        return this;
    }

    /**
     * Return the modification time.
     *
     * @return the value, or null when it is not set
     */
    public Instant modified() {
        return modified;
    }

    /**
     * Set the max-snippet directive value.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues maxSnippet(Integer value) {
        this.maxSnippet = value;
        return this;
    }

    /**
     * Return the max-snippet directive value.
     *
     * @return the value, or null when it is not set
     */
    public Integer maxSnippet() {
        return maxSnippet;
    }

    /**
     * Set the unavailable_after time.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues unavailableAfter(Instant value) {
        this.unavailableAfter = value;
        return this;
    }

    /**
     * Return the unavailable_after time.
     *
     * @return the value, or null when it is not set
     */
    public Instant unavailableAfter() {
        return unavailableAfter;
    }

    /**
     * Set the noindex flag.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues noindex(Boolean value) {
        this.noindex = value;
        return this;
    }

    /**
     * Return the noindex flag.
     *
     * @return the value, or null when it is not set
     */
    public Boolean noindex() {
        return noindex;
    }

    /**
     * Set the nofollow flag.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues nofollow(Boolean value) {
        this.nofollow = value;
        return this;
    }

    /**
     * Return the nofollow flag.
     *
     * @return the value, or null when it is not set
     */
    public Boolean nofollow() {
        return nofollow;
    }

    /**
     * Set the noimageindex flag.
     *
     * @param value the new value, where null or blank clears it
     * @return this object
     */
    public SeoValues noimageindex(Boolean value) {
        this.noimageindex = value;
        return this;
    }

    /**
     * Return the noimageindex flag.
     *
     * @return the value, or null when it is not set
     */
    public Boolean noimageindex() {
        return noimageindex;
    }

    /**
     * Add an alternate page for a locale.
     *
     * @param alternateLocale the locale of the alternate
     * @param path the relative or absolute URL of the alternate, where null or blank changes nothing
     * @return this object
     */
    public SeoValues alternate(Locale alternateLocale, String path) {
        if (alternateLocale != null && path != null && !path.isBlank()) {
            alternates.put(alternateLocale, path);
        }
        return this;
    }

    /**
     * Return the alternates in the order they were added.
     *
     * @return the map from locale to path
     */
    public LinkedHashMap<Locale, String> alternates() {
        return alternates;
    }

    /**
     * Add an article tag.
     *
     * @param tag the tag, where null or blank changes nothing
     * @return this object
     */
    public SeoValues tag(String tag) {
        if (tag != null && !tag.isBlank()) {
            tags.add(tag);
        }
        return this;
    }

    /**
     * Return the article tags.
     *
     * @return the tags
     */
    public List<String> tags() {
        return tags;
    }

    /**
     * Add a JSON-LD node.
     *
     * @param node the node, where null changes nothing
     * @return this object
     */
    public SeoValues jsonLd(SchemaNode node) {
        if (node != null) {
            jsonLdNodes.add(node);
        }
        return this;
    }

    /**
     * Return the JSON-LD nodes.
     *
     * @return the nodes
     */
    public List<SchemaNode> jsonLdNodes() {
        return jsonLdNodes;
    }

    /**
     * Add a raw JSON-LD object.
     *
     * @param json the JSON text, where null or blank changes nothing
     * @return this object
     */
    public SeoValues jsonLdRaw(String json) {
        if (json != null && !json.isBlank()) {
            jsonLdRaw.add(json);
        }
        return this;
    }

    /**
     * Return the raw JSON-LD objects.
     *
     * @return the JSON texts
     */
    public List<String> jsonLdRaw() {
        return jsonLdRaw;
    }

    /**
     * Add an extra meta tag.
     *
     * @param meta the tag, where null changes nothing
     * @return this object
     */
    public SeoValues meta(ResolvedSeo.Meta meta) {
        if (meta != null) {
            extraMeta.add(meta);
        }
        return this;
    }

    /**
     * Return the extra meta tags.
     *
     * @return the tags
     */
    public List<ResolvedSeo.Meta> extraMeta() {
        return extraMeta;
    }

    /**
     * Merge this layer over a lower one.
     *
     * <p>
     * Each field set here replaces the field of the lower layer. The title and the absolute title form one group.
     * The image, its width, its height and its alt text form another group. When this layer sets any field of a
     * group, the whole group comes from this layer. The lists hold the lower entries first.
     *
     * @param lower the layer below this one
     * @return a new object with the merged values
     */
    public SeoValues over(SeoValues lower) {
        SeoValues result = new SeoValues();
        result.copyFrom(lower);
        result.copyFrom(this);
        return result;
    }

    private void copyFrom(SeoValues source) {
        if (source.title != null || source.absoluteTitle != null) {
            title = source.title;
            absoluteTitle = source.absoluteTitle;
        }
        if (source.description != null) {
            description = source.description;
        }
        if (source.canonical != null) {
            canonical = source.canonical;
        }
        if (source.type != null) {
            type = source.type;
        }
        if (source.image != null || source.imageWidth != null || source.imageHeight != null
                || source.imageAlt != null) {
            image = source.image;
            imageWidth = source.imageWidth;
            imageHeight = source.imageHeight;
            imageAlt = source.imageAlt;
        }
        if (source.author != null) {
            author = source.author;
        }
        if (source.section != null) {
            section = source.section;
        }
        if (source.twitterCard != null) {
            twitterCard = source.twitterCard;
        }
        if (source.maxImagePreview != null) {
            maxImagePreview = source.maxImagePreview;
        }
        if (source.locale != null) {
            locale = source.locale;
        }
        if (source.published != null) {
            published = source.published;
        }
        if (source.modified != null) {
            modified = source.modified;
        }
        if (source.maxSnippet != null) {
            maxSnippet = source.maxSnippet;
        }
        if (source.unavailableAfter != null) {
            unavailableAfter = source.unavailableAfter;
        }
        if (source.noindex != null) {
            noindex = source.noindex;
        }
        if (source.nofollow != null) {
            nofollow = source.nofollow;
        }
        if (source.noimageindex != null) {
            noimageindex = source.noimageindex;
        }
        alternates.putAll(source.alternates);
        tags.addAll(source.tags);
        jsonLdNodes.addAll(source.jsonLdNodes);
        jsonLdRaw.addAll(source.jsonLdRaw);
        extraMeta.addAll(source.extraMeta);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
