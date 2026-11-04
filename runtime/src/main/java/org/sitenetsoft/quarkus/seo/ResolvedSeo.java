package org.sitenetsoft.quarkus.seo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Hold the final values of the head tags of one page.
 *
 * @param title the text of the title element
 * @param description the description
 * @param robots the robots directives in output order
 * @param canonical the absolute canonical URL, which also serves as og:url
 * @param hreflang the alternate language links
 * @param type the Open Graph type
 * @param ogTitle the Open Graph title
 * @param siteName the Open Graph site name
 * @param ogLocale the Open Graph locale
 * @param ogLocaleAlternates the alternate Open Graph locales
 * @param image the image
 * @param article the article values, present only for the article type
 * @param twitterCard the Twitter card type
 * @param twitterSite the twitter:site handle
 * @param twitterCreator the twitter:creator handle
 * @param verification the site verification tags
 * @param themeColor the theme color
 * @param extraMeta the extra meta tags
 * @param jsonLd the JSON-LD text, or an empty string when there is none
 * @param manifest the href of the manifest link, or empty when the page writes none
 */
public record ResolvedSeo(
        String title,
        Optional<String> description,
        List<String> robots,
        Optional<String> canonical,
        List<Link> hreflang,
        String type,
        Optional<String> ogTitle,
        Optional<String> siteName,
        String ogLocale,
        List<String> ogLocaleAlternates,
        Optional<Image> image,
        Optional<Article> article,
        String twitterCard,
        Optional<String> twitterSite,
        Optional<String> twitterCreator,
        List<Meta> verification,
        Optional<String> themeColor,
        List<Meta> extraMeta,
        String jsonLd,
        Optional<String> manifest) {

    /**
     * Hold one alternate language link.
     *
     * @param hreflang the language tag or {@code x-default}
     * @param href the absolute URL
     */
    public record Link(String hreflang, String href) {
    }

    /**
     * Hold the image of a page.
     *
     * @param url the absolute URL
     * @param width the width in pixels, or null
     * @param height the height in pixels, or null
     * @param alt the alt text, or null
     */
    public record Image(String url, Integer width, Integer height, String alt) {
    }

    /**
     * Hold the article values.
     *
     * @param published the publication time
     * @param modified the modification time
     * @param author the author
     * @param section the section
     * @param tags the tags
     */
    public record Article(Optional<Instant> published, Optional<Instant> modified, Optional<String> author,
            Optional<String> section, List<String> tags) {
    }

    /**
     * Hold one meta tag.
     *
     * @param attribute the attribute that holds the key, either {@code name} or {@code property}
     * @param key the name or property
     * @param content the content
     */
    public record Meta(String attribute, String key, String content) {
    }
}
