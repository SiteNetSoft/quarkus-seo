package org.sitenetsoft.quarkus.seo.runtime;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Article;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Image;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Link;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Meta;
import org.sitenetsoft.quarkus.seo.SeoHeadRenderer;

/**
 * Write the head tags in a fixed order and escape every value.
 */
@DefaultBean
@Singleton
public class DefaultSeoHeadRenderer implements SeoHeadRenderer {

    @Override
    public String render(ResolvedSeo seo) {
        StringBuilder out = new StringBuilder();
        out.append("<title>").append(Html.escape(seo.title())).append("</title>\n");
        meta(out, "name", "description", seo.description().orElse(null));
        if (!seo.robots().isEmpty()) {
            meta(out, "name", "robots", String.join(", ", seo.robots()));
        }
        seo.canonical().ifPresent(url -> link(out, "canonical", null, url));
        for (Link alternate : seo.hreflang()) {
            link(out, "alternate", alternate.hreflang(), alternate.href());
        }
        openGraph(out, seo);
        seo.article().ifPresent(article -> article(out, article));
        meta(out, "name", "twitter:card", seo.twitterCard());
        meta(out, "name", "twitter:site", seo.twitterSite().orElse(null));
        meta(out, "name", "twitter:creator", seo.twitterCreator().orElse(null));
        metas(out, seo.verification());
        meta(out, "name", "theme-color", seo.themeColor().orElse(null));
        metas(out, seo.extraMeta());
        if (seo.jsonLd() != null && !seo.jsonLd().isBlank()) {
            out.append("<script type=\"application/ld+json\">").append(seo.jsonLd()).append("</script>\n");
        }
        return out.toString();
    }

    private static void openGraph(StringBuilder out, ResolvedSeo seo) {
        meta(out, "property", "og:type", seo.type());
        meta(out, "property", "og:title", seo.ogTitle().orElse(null));
        meta(out, "property", "og:description", seo.description().orElse(null));
        meta(out, "property", "og:url", seo.canonical().orElse(null));
        meta(out, "property", "og:site_name", seo.siteName().orElse(null));
        meta(out, "property", "og:locale", seo.ogLocale());
        for (String alternate : seo.ogLocaleAlternates()) {
            meta(out, "property", "og:locale:alternate", alternate);
        }
        Optional<Image> image = seo.image();
        if (image.isPresent()) {
            Image value = image.get();
            meta(out, "property", "og:image", value.url());
            meta(out, "property", "og:image:width", value.width() == null ? null : value.width().toString());
            meta(out, "property", "og:image:height", value.height() == null ? null : value.height().toString());
            meta(out, "property", "og:image:alt", value.alt());
        }
    }

    private static void article(StringBuilder out, Article article) {
        meta(out, "property", "article:published_time", format(article.published()));
        meta(out, "property", "article:modified_time", format(article.modified()));
        meta(out, "property", "article:author", article.author().orElse(null));
        meta(out, "property", "article:section", article.section().orElse(null));
        for (String tag : article.tags()) {
            meta(out, "property", "article:tag", tag);
        }
    }

    private static String format(Optional<Instant> time) {
        return time.isPresent() ? DateTimeFormatter.ISO_INSTANT.format(time.get()) : null;
    }

    private static void metas(StringBuilder out, List<Meta> metas) {
        for (Meta meta : metas) {
            meta(out, meta.attribute(), meta.key(), meta.content());
        }
    }

    private static void meta(StringBuilder out, String attribute, String key, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        out.append("<meta ").append(Html.escape(attribute)).append("=\"").append(Html.escape(key))
                .append("\" content=\"").append(Html.escape(content)).append("\">\n");
    }

    private static void link(StringBuilder out, String rel, String hreflang, String href) {
        out.append("<link rel=\"").append(rel).append('"');
        if (hreflang != null) {
            out.append(" hreflang=\"").append(Html.escape(hreflang)).append('"');
        }
        out.append(" href=\"").append(Html.escape(href)).append("\">\n");
    }
}
