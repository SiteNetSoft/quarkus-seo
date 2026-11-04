package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Article;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Image;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Link;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Meta;

class DefaultSeoHeadRendererTest {

    private final DefaultSeoHeadRenderer renderer = new DefaultSeoHeadRenderer();

    private static ResolvedSeo minimal(String title) {
        return new ResolvedSeo(title, Optional.empty(), List.of(), Optional.empty(), List.of(), "website",
                Optional.of(title), Optional.empty(), "en_US", List.of(), Optional.empty(), Optional.empty(),
                "summary", Optional.empty(), Optional.empty(), List.of(), Optional.empty(), List.of(), "");
    }

    private static ResolvedSeo full() {
        return new ResolvedSeo("Post", Optional.of("About a post"), List.of("index", "follow", "max-image-preview:large"),
                Optional.of("https://example.com/en/post"),
                List.of(new Link("en", "https://example.com/en/post"), new Link("fr", "https://example.com/fr/post"),
                        new Link("x-default", "https://example.com/en/post")),
                "article", Optional.of("Post title"), Optional.of("Example"), "en_US", List.of("fr_FR", "de_DE"),
                Optional.of(new Image("https://example.com/i.png", 1200, 630, "An image")),
                Optional.of(new Article(Optional.of(Instant.parse("2026-01-02T03:04:05Z")),
                        Optional.of(Instant.parse("2026-02-03T04:05:06Z")), Optional.of("Ann"), Optional.of("News"),
                        List.of("a", "b"))),
                "summary_large_image", Optional.of("@site"), Optional.of("@ann"),
                List.of(new Meta("name", "google-site-verification", "abc")), Optional.of("#ffffff"),
                List.of(new Meta("property", "fb:app_id", "42")), "{\"@type\":\"Article\"}");
    }

    @Test
    void fullPageInFixedOrder() {
        String expected = """
                <title>Post</title>
                <meta name="description" content="About a post">
                <meta name="robots" content="index, follow, max-image-preview:large">
                <link rel="canonical" href="https://example.com/en/post">
                <link rel="alternate" hreflang="en" href="https://example.com/en/post">
                <link rel="alternate" hreflang="fr" href="https://example.com/fr/post">
                <link rel="alternate" hreflang="x-default" href="https://example.com/en/post">
                <meta property="og:type" content="article">
                <meta property="og:title" content="Post title">
                <meta property="og:description" content="About a post">
                <meta property="og:url" content="https://example.com/en/post">
                <meta property="og:site_name" content="Example">
                <meta property="og:locale" content="en_US">
                <meta property="og:locale:alternate" content="fr_FR">
                <meta property="og:locale:alternate" content="de_DE">
                <meta property="og:image" content="https://example.com/i.png">
                <meta property="og:image:width" content="1200">
                <meta property="og:image:height" content="630">
                <meta property="og:image:alt" content="An image">
                <meta property="article:published_time" content="2026-01-02T03:04:05Z">
                <meta property="article:modified_time" content="2026-02-03T04:05:06Z">
                <meta property="article:author" content="Ann">
                <meta property="article:section" content="News">
                <meta property="article:tag" content="a">
                <meta property="article:tag" content="b">
                <meta name="twitter:card" content="summary_large_image">
                <meta name="twitter:site" content="@site">
                <meta name="twitter:creator" content="@ann">
                <meta name="google-site-verification" content="abc">
                <meta name="theme-color" content="#ffffff">
                <meta property="fb:app_id" content="42">
                <script type="application/ld+json">{"@type":"Article"}</script>
                """;
        assertThat(renderer.render(full())).isEqualTo(expected);
    }

    @Test
    void absentValuesWriteNoTag() {
        String html = renderer.render(minimal("Home"));
        assertThat(html).isEqualTo("<title>Home</title>\n<meta property=\"og:type\" content=\"website\">\n"
                + "<meta property=\"og:title\" content=\"Home\">\n<meta property=\"og:locale\" content=\"en_US\">\n"
                + "<meta name=\"twitter:card\" content=\"summary\">\n");
        assertThat(html).doesNotContain("content=\"\"");
    }

    @Test
    void escapesEveryAttributeAndTheTitle() {
        String html = renderer.render(minimal("<script>\"x\" & 'y' 100% {site-name}"));
        assertThat(html).contains("<title>&lt;script&gt;&quot;x&quot; &amp; &#39;y&#39; 100% {site-name}</title>");
        assertThat(html).contains("content=\"&lt;script&gt;&quot;x&quot; &amp; &#39;y&#39; 100% {site-name}\"");
    }

    @Test
    void imageWithoutSizeOrAltWritesOnlyTheUrl() {
        ResolvedSeo seo = withImage(new Image("https://example.com/i.png", null, null, " "));
        assertThat(renderer.render(seo)).contains("og:image\" content=\"https://example.com/i.png\">\n")
                .doesNotContain("og:image:");
    }

    @Test
    void blankTitleWritesNoTitleElement() {
        assertThat(renderer.render(minimal(""))).doesNotContain("<title>");
        assertThat(renderer.render(minimal("  "))).doesNotContain("<title>");
    }

    @Test
    void emptyJsonLdWritesNoScript() {
        assertThat(renderer.render(minimal("Home"))).doesNotContain("<script");
    }

    private static ResolvedSeo withImage(Image image) {
        ResolvedSeo m = minimal("Home");
        return new ResolvedSeo(m.title(), m.description(), m.robots(), m.canonical(), m.hreflang(), m.type(),
                m.ogTitle(), m.siteName(), m.ogLocale(), m.ogLocaleAlternates(), Optional.of(image), m.article(),
                m.twitterCard(), m.twitterSite(), m.twitterCreator(), m.verification(), m.themeColor(),
                m.extraMeta(), m.jsonLd());
    }
}
