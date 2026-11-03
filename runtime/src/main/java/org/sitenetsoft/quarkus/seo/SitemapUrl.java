package org.sitenetsoft.quarkus.seo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Describe one URL of a sitemap.
 */
public final class SitemapUrl {

    private final String loc;
    private Instant lastmod;
    private ChangeFrequency changefreq;
    private Double priority;
    private final Map<Locale, String> alternates = new LinkedHashMap<>();
    private final List<String> images = new ArrayList<>();
    private String title;

    private SitemapUrl(String loc) {
        this.loc = loc;
    }

    /**
     * Start a sitemap URL.
     *
     * @param loc the path or absolute URL
     * @return the new sitemap URL
     */
    public static SitemapUrl of(String loc) {
        if (loc == null) {
            throw new IllegalArgumentException("loc must not be null");
        }
        return new SitemapUrl(loc);
    }

    /**
     * Set the time of the last change.
     *
     * @param lastmod the time of the last change
     * @return this URL
     */
    public SitemapUrl lastmod(Instant lastmod) {
        this.lastmod = lastmod;
        return this;
    }

    /**
     * Set the change frequency.
     *
     * @param changefreq the change frequency
     * @return this URL
     */
    public SitemapUrl changefreq(ChangeFrequency changefreq) {
        this.changefreq = changefreq;
        return this;
    }

    /**
     * Set the priority.
     *
     * @param priority the priority from 0.0 to 1.0
     * @return this URL
     * @throws IllegalArgumentException when the priority is outside 0.0 to 1.0
     */
    public SitemapUrl priority(double priority) {
        if (!(priority >= 0.0 && priority <= 1.0)) {
            throw new IllegalArgumentException("priority must be between 0.0 and 1.0: " + priority);
        }
        this.priority = priority;
        return this;
    }

    /**
     * Add a language version of this page.
     *
     * @param locale the language of the version
     * @param url the path or absolute URL of the version
     * @return this URL
     */
    public SitemapUrl alternate(Locale locale, String url) {
        alternates.put(locale, url);
        return this;
    }

    /**
     * Add an image of this page.
     *
     * @param url the path or absolute URL of the image
     * @return this URL
     */
    public SitemapUrl image(String url) {
        images.add(url);
        return this;
    }

    /**
     * Set the page title for contributors that need one.
     *
     * @param title the page title
     * @return this URL
     */
    public SitemapUrl title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Return the path or absolute URL.
     *
     * @return the location
     */
    public String loc() {
        return loc;
    }

    /**
     * Return the time of the last change.
     *
     * @return the time, or empty
     */
    public Optional<Instant> lastmod() {
        return Optional.ofNullable(lastmod);
    }

    /**
     * Return the change frequency.
     *
     * @return the change frequency, or empty
     */
    public Optional<ChangeFrequency> changefreq() {
        return Optional.ofNullable(changefreq);
    }

    /**
     * Return the priority.
     *
     * @return the priority, or empty
     */
    public OptionalDouble priority() {
        return priority == null ? OptionalDouble.empty() : OptionalDouble.of(priority);
    }

    /**
     * Return the language versions in the order they were added.
     *
     * @return an unmodifiable map
     */
    public Map<Locale, String> alternates() {
        return Collections.unmodifiableMap(alternates);
    }

    /**
     * Return the images.
     *
     * @return an unmodifiable list
     */
    public List<String> images() {
        return Collections.unmodifiableList(images);
    }

    /**
     * Return the page title.
     *
     * @return the title, or empty
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }
}
