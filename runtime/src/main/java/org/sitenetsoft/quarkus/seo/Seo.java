package org.sitenetsoft.quarkus.seo;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.runtime.RequestInfo;
import org.sitenetsoft.quarkus.seo.runtime.RobotsHeader;
import org.sitenetsoft.quarkus.seo.runtime.SeoResolver;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;
import org.sitenetsoft.quarkus.seo.runtime.SeoValues;
import org.sitenetsoft.quarkus.seo.schema.SchemaNode;

/**
 * Set the SEO values of the current page from Java.
 *
 * <p>
 * Values set here win over the values of the {@code {#seo}} tag and over the configuration.
 */
@RequestScoped
public class Seo {

    private final SeoValues java = new SeoValues();
    private SeoValues tag = new SeoValues();

    @Inject
    SeoResolver resolver;

    @Inject
    BaseUrlResolver baseUrlResolver;

    @Inject
    SeoSettings settings;

    @Inject
    SeoHeadRenderer renderer;

    /**
     * Set the page title. The title template applies to it.
     *
     * @param title the title
     * @return this object
     */
    public Seo title(String title) {
        java.title(title);
        return this;
    }

    /**
     * Set a title that skips the title template.
     *
     * @param title the full title
     * @return this object
     */
    public Seo absoluteTitle(String title) {
        java.absoluteTitle(title);
        return this;
    }

    /**
     * Set the description.
     *
     * @param description the description
     * @return this object
     */
    public Seo description(String description) {
        java.description(description);
        return this;
    }

    /**
     * Set the canonical URL.
     *
     * @param canonical a relative or absolute URL
     * @return this object
     */
    public Seo canonical(String canonical) {
        java.canonical(canonical);
        return this;
    }

    /**
     * Set the Open Graph type.
     *
     * @param type the type, for example {@code article}
     * @return this object
     */
    public Seo type(String type) {
        java.type(type);
        return this;
    }

    /**
     * Set the image.
     *
     * @param image a relative or absolute URL
     * @return this object
     */
    public Seo image(String image) {
        java.image(image);
        return this;
    }

    /**
     * Set the image with its size.
     *
     * @param image a relative or absolute URL
     * @param width the width in pixels
     * @param height the height in pixels
     * @return this object
     */
    public Seo image(String image, int width, int height) {
        java.image(image).imageWidth(width).imageHeight(height);
        return this;
    }

    /**
     * Set the image alt text.
     *
     * @param alt the alt text
     * @return this object
     */
    public Seo imageAlt(String alt) {
        java.imageAlt(alt);
        return this;
    }

    /**
     * Set the page locale.
     *
     * @param locale the locale
     * @return this object
     */
    public Seo locale(Locale locale) {
        java.locale(locale);
        return this;
    }

    /**
     * Add an alternate page for a locale.
     *
     * @param locale the locale of the alternate
     * @param path a relative or absolute URL
     * @return this object
     */
    public Seo alternate(Locale locale, String path) {
        java.alternate(locale, path);
        return this;
    }

    /**
     * Ask search engines not to index the page.
     *
     * @return this object
     */
    public Seo noindex() {
        java.noindex(true);
        return publishRobots();
    }

    /**
     * Ask search engines not to follow the links of the page.
     *
     * @return this object
     */
    public Seo nofollow() {
        java.nofollow(true);
        return publishRobots();
    }

    /**
     * Ask search engines not to index the images of the page.
     *
     * @return this object
     */
    public Seo noimageindex() {
        java.noimageindex(true);
        return publishRobots();
    }

    /**
     * Limit the length of the text snippet.
     *
     * @param characters the maximum number of characters
     * @return this object
     */
    public Seo maxSnippet(int characters) {
        java.maxSnippet(characters);
        return publishRobots();
    }

    /**
     * Limit the size of the image preview.
     *
     * @param size one of {@code none}, {@code standard} and {@code large}
     * @return this object
     */
    public Seo maxImagePreview(String size) {
        java.maxImagePreview(size);
        return publishRobots();
    }

    /**
     * Ask search engines to drop the page after a time.
     *
     * @param time the time
     * @return this object
     */
    public Seo unavailableAfter(Instant time) {
        java.unavailableAfter(time);
        return publishRobots();
    }

    /**
     * Set the publication time.
     *
     * @param time the time
     * @return this object
     */
    public Seo published(Instant time) {
        java.published(time);
        return this;
    }

    /**
     * Set the modification time.
     *
     * @param time the time
     * @return this object
     */
    public Seo modified(Instant time) {
        java.modified(time);
        return this;
    }

    /**
     * Set the article author.
     *
     * @param author the author
     * @return this object
     */
    public Seo author(String author) {
        java.author(author);
        return this;
    }

    /**
     * Set the article section.
     *
     * @param section the section
     * @return this object
     */
    public Seo section(String section) {
        java.section(section);
        return this;
    }

    /**
     * Add article tags.
     *
     * @param tags the tags
     * @return this object
     */
    public Seo tag(String... tags) {
        for (String value : tags) {
            java.tag(value);
        }
        return this;
    }

    /**
     * Set the Twitter card type.
     *
     * @param card the card type
     * @return this object
     */
    public Seo twitterCard(String card) {
        java.twitterCard(card);
        return this;
    }

    /**
     * Add a JSON-LD node.
     *
     * @param node the node
     * @return this object
     */
    public Seo jsonLd(SchemaNode node) {
        java.jsonLd(node);
        return this;
    }

    /**
     * Add a JSON-LD object that is already written as JSON.
     *
     * @param json the JSON text
     * @return this object
     */
    public Seo jsonLdRaw(String json) {
        java.jsonLdRaw(json);
        return this;
    }

    /**
     * Add a meta tag with a name attribute.
     *
     * @param name the name
     * @param content the content
     * @return this object
     */
    public Seo meta(String name, String content) {
        java.meta(new ResolvedSeo.Meta("name", name, content));
        return this;
    }

    /**
     * Add a meta tag with a property attribute.
     *
     * @param property the property
     * @param content the content
     * @return this object
     */
    public Seo property(String property, String content) {
        java.meta(new ResolvedSeo.Meta("property", property, content));
        return this;
    }

    /**
     * Copy the values of a source into the Java layer.
     *
     * @param source the source
     * @return this object
     */
    public Seo from(SeoSource source) {
        source.title().ifPresent(this::title);
        source.description().ifPresent(this::description);
        source.canonical().ifPresent(this::canonical);
        source.type().ifPresent(this::type);
        source.image().ifPresent(this::image);
        source.imageAlt().ifPresent(this::imageAlt);
        source.locale().ifPresent(this::locale);
        source.published().ifPresent(this::published);
        source.modified().ifPresent(this::modified);
        source.author().ifPresent(this::author);
        source.section().ifPresent(this::section);
        if (source.noindex().orElse(false)) {
            noindex();
        }
        for (Map.Entry<Locale, String> entry : source.alternates().entrySet()) {
            alternate(entry.getKey(), entry.getValue());
        }
        List<String> sourceTags = source.tags();
        tag(sourceTags.toArray(new String[0]));
        return this;
    }

    /**
     * Resolve the values of the current page.
     *
     * @return the resolved values
     */
    public ResolvedSeo resolve() {
        return resolver.resolve(tag, java, RequestInfo.current(baseUrlResolver, settings));
    }

    /**
     * Render the head tags of the current page.
     *
     * @return the head tags, one per line
     */
    public String head() {
        return renderer.render(resolve());
    }

    /**
     * Return the values set in Java.
     *
     * @return the Java layer
     */
    public SeoValues javaValues() {
        return java;
    }

    /**
     * Return the values from the tag parameters.
     *
     * @return the tag layer
     */
    public SeoValues tagValues() {
        return tag;
    }

    /**
     * Store the values from the tag parameters.
     *
     * @param values the tag layer
     */
    public void tagValues(SeoValues values) {
        this.tag = values == null ? new SeoValues() : values;
    }

    private Seo publishRobots() {
        InstanceHandle<CurrentVertxRequest> handle = Arc.container() == null ? null
                : Arc.container().instance(CurrentVertxRequest.class);
        if (handle != null && handle.isAvailable()) {
            RoutingContext context = handle.get().getCurrent();
            if (context != null) {
                RobotsHeader.put(context, SeoResolver.directives(java));
            }
        }
        return this;
    }
}
