package org.sitenetsoft.quarkus.seo.it;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.sitenetsoft.quarkus.seo.SeoSource;

/**
 * Describe a content page that supplies its own SEO values.
 */
public final class CmsPage implements SeoSource {

    private final String slug;
    private final String headline;
    private final String summary;
    private final Instant publishedAt;

    /**
     * Create a page.
     *
     * @param slug the page slug
     * @param headline the page title
     * @param summary the page summary
     * @param publishedAt the publication time
     */
    public CmsPage(String slug, String headline, String summary, Instant publishedAt) {
        this.slug = slug;
        this.headline = headline;
        this.summary = summary;
        this.publishedAt = publishedAt;
    }

    /**
     * Return the page title.
     *
     * @return the title
     */
    @Override
    public Optional<String> title() {
        return Optional.of(headline);
    }

    /**
     * Return the page summary as the description.
     *
     * @return the description
     */
    @Override
    public Optional<String> description() {
        return Optional.of(summary);
    }

    /**
     * Return the Open Graph image of the page.
     *
     * @return the image path
     */
    @Override
    public Optional<String> image() {
        return Optional.of("/img/" + slug + ".png");
    }

    /**
     * Return the publication time.
     *
     * @return the time
     */
    @Override
    public Optional<Instant> published() {
        return Optional.of(publishedAt);
    }

    /**
     * Return the French alternate of the page.
     *
     * @return the alternates by locale
     */
    @Override
    public Map<Locale, String> alternates() {
        return Map.of(Locale.FRENCH, "/fr/" + slug);
    }
}
