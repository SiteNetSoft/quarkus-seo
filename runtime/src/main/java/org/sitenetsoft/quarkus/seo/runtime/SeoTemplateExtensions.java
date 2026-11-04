package org.sitenetsoft.quarkus.seo.runtime;

import java.util.Locale;
import java.util.Optional;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.qute.TemplateExtension;
import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.Seo;

/**
 * Give templates single resolved values through the {@code seo} namespace.
 *
 * <p>
 * Each method returns an empty string when the value is absent.
 */
@TemplateExtension(namespace = "seo")
public class SeoTemplateExtensions {

    /**
     * Return the text of the title element.
     *
     * @return the title, or an empty string
     */
    public static String title() {
        return orEmpty(resolved().title());
    }

    /**
     * Return the description.
     *
     * @return the description, or an empty string
     */
    public static String description() {
        return resolved().description().orElse("");
    }

    /**
     * Return the absolute canonical URL.
     *
     * @return the canonical URL, or an empty string
     */
    public static String canonical() {
        return resolved().canonical().orElse("");
    }

    /**
     * Return the absolute image URL.
     *
     * @return the image URL, or an empty string
     */
    public static String image() {
        Optional<ResolvedSeo.Image> image = resolved().image();
        return image.isPresent() ? orEmpty(image.get().url()) : "";
    }

    /**
     * Return the page locale as a BCP 47 language tag.
     *
     * @return the language tag, or an empty string
     */
    public static String locale() {
        return languageTag(resolved().ogLocale());
    }

    /**
     * Return the site name.
     *
     * @return the site name, or an empty string
     */
    public static String siteName() {
        return resolved().siteName().orElse("");
    }

    /**
     * Turn an Open Graph locale such as {@code fr_CA} into a language tag such as {@code fr-CA}.
     */
    private static String languageTag(String ogLocale) {
        if (ogLocale == null || ogLocale.isBlank()) {
            return "";
        }
        String[] parts = ogLocale.split("_", 3);
        Locale locale = switch (parts.length) {
            case 1 -> Locale.of(parts[0]);
            case 2 -> Locale.of(parts[0], parts[1]);
            default -> Locale.of(parts[0], parts[1], parts[2]);
        };
        return locale.toLanguageTag();
    }

    /**
     * Resolve the values of the current page.
     *
     * <p>
     * With an active request context, the request {@code Seo} bean holds the tag and Java values. With no request
     * context, only the configuration applies.
     */
    private static ResolvedSeo resolved() {
        ArcContainer container = Arc.container();
        if (container.requestContext().isActive()) {
            return container.instance(Seo.class).get().resolve();
        }
        return container.instance(SeoResolver.class).get().resolve(new SeoValues(), new SeoValues(),
                Optional.empty());
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
