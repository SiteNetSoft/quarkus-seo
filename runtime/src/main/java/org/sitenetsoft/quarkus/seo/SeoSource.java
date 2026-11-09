package org.sitenetsoft.quarkus.seo;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Supply page values from an object such as a content entity.
 *
 * <p>
 * Every method has a default that supplies nothing, so an object implements only the values it has.
 */
public interface SeoSource {

    /**
     * Supply the page title.
     *
     * @return the title
     */
    default Optional<String> title() {
        return Optional.empty();
    }

    /**
     * Supply the description.
     *
     * @return the description
     */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Supply the canonical URL or path.
     *
     * @return the canonical value
     */
    default Optional<String> canonical() {
        return Optional.empty();
    }

    /**
     * Supply the Open Graph type.
     *
     * @return the type
     */
    default Optional<String> type() {
        return Optional.empty();
    }

    /**
     * Supply the image URL or path.
     *
     * @return the image
     */
    default Optional<String> image() {
        return Optional.empty();
    }

    /**
     * Supply the image alt text.
     *
     * @return the alt text
     */
    default Optional<String> imageAlt() {
        return Optional.empty();
    }

    /**
     * Supply the page locale.
     *
     * @return the locale
     */
    default Optional<Locale> locale() {
        return Optional.empty();
    }

    /**
     * Supply the publication time.
     *
     * @return the time
     */
    default Optional<Instant> published() {
        return Optional.empty();
    }

    /**
     * Supply the modification time.
     *
     * @return the time
     */
    default Optional<Instant> modified() {
        return Optional.empty();
    }

    /**
     * Supply the author.
     *
     * @return the author
     */
    default Optional<String> author() {
        return Optional.empty();
    }

    /**
     * Supply the section.
     *
     * @return the section
     */
    default Optional<String> section() {
        return Optional.empty();
    }

    /**
     * Supply the noindex flag.
     *
     * @return the flag
     */
    default Optional<Boolean> noindex() {
        return Optional.empty();
    }

    /**
     * Supply the alternate pages.
     *
     * @return the map from locale to path
     */
    default Map<Locale, String> alternates() {
        return Map.of();
    }

    /**
     * Supply the article tags.
     *
     * @return the tags
     */
    default List<String> tags() {
        return List.of();
    }
}
