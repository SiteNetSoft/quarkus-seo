package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Link;
import org.sitenetsoft.quarkus.seo.SeoLocales;
import org.sitenetsoft.quarkus.seo.schema.SchemaNode;

class SeoResolverTest {

    private static final String BASE = "https://ex.com";

    private static final Optional<RequestInfo> ABOUT = Optional.of(
            new RequestInfo(BASE, "/about", "utm=1&page=2", false));

    private static SeoLocales locales(Locale defaultLocale) {
        return new SeoLocales() {
            @Override
            public List<Locale> locales() {
                return List.of(defaultLocale);
            }

            @Override
            public Locale defaultLocale() {
                return defaultLocale;
            }
        };
    }

    private static SeoResolver resolver(SeoSettings settings, Locale defaultLocale, TestConfig config) {
        return new SeoResolver(settings, locales(defaultLocale), config.config(), config.buildConfig());
    }

    private static SeoSettings settings(boolean indexable) {
        return new SeoSettings(Optional.of("Acme"), indexable, null, Optional.empty(), "");
    }

    private static SeoResolver resolver() {
        return resolver(settings(true), Locale.ENGLISH, new TestConfig().with("canonical.keepQueryParams", List.of("page")));
    }

    private static ResolvedSeo resolve(SeoValues tag, SeoValues java) {
        return resolver().resolve(tag, java, ABOUT);
    }

    @Test
    void javaWinsOverTagWhichWinsOverConfig() {
        SeoResolver resolver = resolver(settings(true), Locale.ENGLISH, new TestConfig().with("description", "C"));
        SeoValues tag = new SeoValues().description("T");
        assertThat(resolver.resolve(tag, new SeoValues().description("J"), ABOUT).description()).contains("J");
        assertThat(resolver.resolve(tag, new SeoValues(), ABOUT).description()).contains("T");
        assertThat(resolver.resolve(new SeoValues(), new SeoValues(), ABOUT).description()).contains("C");
    }

    @Test
    void titleTemplateAppliesToPageTitleOnly() {
        assertThat(resolve(new SeoValues(), new SeoValues().title("About")).title()).isEqualTo("About | Acme");
        assertThat(resolve(new SeoValues(), new SeoValues()).title()).isEqualTo("Acme");
        assertThat(resolve(new SeoValues(), new SeoValues().absoluteTitle("Exact")).title()).isEqualTo("Exact");
        assertThat(resolve(new SeoValues(), new SeoValues().title("About")).ogTitle()).contains("About");
    }

    @Test
    void titleWithPercentAndPlaceholderDoesNotThrow() {
        assertThat(resolve(new SeoValues(), new SeoValues().title("100% {site-name}")).title())
                .isEqualTo("100% {site-name} | Acme");
    }

    @Test
    void noSiteNameMeansPlainTemplate() {
        SeoSettings settings = new SeoSettings(Optional.empty(), true, null, Optional.empty(), "");
        SeoResolver resolver = resolver(settings, Locale.ENGLISH, new TestConfig());
        assertThat(resolver.resolve(new SeoValues(), new SeoValues().title("About"), ABOUT).title())
                .isEqualTo("About");
    }

    @Test
    void canonicalDropsUnlistedQueryParams() {
        assertThat(resolve(new SeoValues(), new SeoValues()).canonical()).contains("https://ex.com/about?page=2");
    }

    @Test
    void relativeCanonicalAndImageBecomeAbsolute() {
        ResolvedSeo seo = resolve(new SeoValues(), new SeoValues().canonical("/x").image("/i.png"));
        assertThat(seo.canonical()).contains("https://ex.com/x");
        assertThat(seo.image().orElseThrow().url()).isEqualTo("https://ex.com/i.png");
    }

    @Test
    void hreflangHasSelfAlternatesAndXDefault() {
        SeoValues java = new SeoValues().locale(Locale.FRENCH).alternate(Locale.ENGLISH, "/en/about");
        assertThat(resolve(new SeoValues(), java).hreflang()).containsExactly(
                new Link("fr", "https://ex.com/about?page=2"),
                new Link("en", "https://ex.com/en/about"),
                new Link("x-default", "https://ex.com/en/about"));
    }

    @Test
    void noAlternatesMeansNoHreflang() {
        assertThat(resolve(new SeoValues(), new SeoValues().locale(Locale.FRENCH)).hreflang()).isEmpty();
    }

    @Test
    void xDefaultIsLeftOutWhenNoLinkHasTheDefaultLocale() {
        SeoValues java = new SeoValues().locale(Locale.FRENCH).alternate(Locale.GERMAN, "/de/about");
        assertThat(resolve(new SeoValues(), java).hreflang()).extracting(Link::hreflang)
                .containsExactly("fr", "de");
    }

    @Test
    void ogLocaleUsesUnderscore() {
        ResolvedSeo us = resolve(new SeoValues(), new SeoValues().locale(Locale.forLanguageTag("en-US"))
                .alternate(Locale.FRENCH, "/fr"));
        assertThat(us.ogLocale()).isEqualTo("en_US");
        assertThat(us.ogLocaleAlternates()).containsExactly("fr");
        assertThat(resolve(new SeoValues(), new SeoValues().locale(Locale.FRENCH)).ogLocale()).isEqualTo("fr");
    }

    @Test
    void twitterCardDependsOnImage() {
        assertThat(resolve(new SeoValues(), new SeoValues().image("/i.png")).twitterCard())
                .isEqualTo("summary_large_image");
        assertThat(resolve(new SeoValues(), new SeoValues()).twitterCard()).isEqualTo("summary");
        assertThat(resolve(new SeoValues(), new SeoValues().image("/i.png").twitterCard("player")).twitterCard())
                .isEqualTo("player");
    }

    @Test
    void notIndexableForcesNoindexNofollow() {
        SeoResolver resolver = resolver(settings(false), Locale.ENGLISH, new TestConfig());
        assertThat(resolver.resolve(new SeoValues(), new SeoValues().maxSnippet(5), ABOUT).robots())
                .containsExactly("noindex", "nofollow");
    }

    @Test
    void robotsDirectivesInFixedOrder() {
        SeoValues java = new SeoValues().unavailableAfter(Instant.parse("2027-01-01T00:00:00Z"))
                .maxImagePreview("large").maxSnippet(50).noimageindex(true).noindex(true);
        assertThat(resolve(new SeoValues(), java).robots()).containsExactly("noindex", "noimageindex",
                "max-snippet:50", "max-image-preview:large", "unavailable_after: 2027-01-01T00:00:00Z");
    }

    @Test
    void articleFieldsOnlyForArticleType() {
        SeoValues java = new SeoValues().author("Ann").tag("a").tag("b");
        assertThat(resolve(new SeoValues(), java).article()).isEmpty();
        ResolvedSeo article = resolve(new SeoValues(), java.type("article"));
        assertThat(article.article().orElseThrow().author()).contains("Ann");
        assertThat(article.article().orElseThrow().tags()).containsExactly("a", "b");
    }

    private static SeoResolver siteResolver() {
        return resolver(settings(true), Locale.ENGLISH, new TestConfig().with("organization.name", "Acme Inc"));
    }

    @Test
    void homePageGetsOrganizationAndWebSite() {
        Optional<RequestInfo> home = Optional.of(new RequestInfo(BASE, "/", null, true));
        String json = siteResolver().resolve(new SeoValues(), new SeoValues(), home).jsonLd();
        assertThat(json).contains("\"@type\":\"Organization\",\"@id\":\"https://ex.com/#organization\"");
        assertThat(json).contains("\"@type\":\"WebSite\",\"@id\":\"https://ex.com/#website\"");
        assertThat(json).contains("\"publisher\":{\"@id\":\"https://ex.com/#organization\"}");
    }

    @Test
    void otherPagesGetNoSiteWideNodes() {
        assertThat(siteResolver().resolve(new SeoValues(), new SeoValues(), ABOUT).jsonLd()).isEmpty();
        String json = siteResolver().resolve(new SeoValues(), new SeoValues().jsonLd(SchemaNode.of("Product")), ABOUT)
                .jsonLd();
        assertThat(json).contains("Product").doesNotContain("Organization");
    }

    @Test
    void jsonLdSwitchedOffGivesEmptyString() {
        TestConfig config = new TestConfig().with("organization.name", "Acme Inc").with("jsonLd.enabled", false);
        SeoResolver resolver = resolver(settings(true), Locale.ENGLISH, config);
        Optional<RequestInfo> home = Optional.of(new RequestInfo(BASE, "/", null, true));
        assertThat(resolver.resolve(new SeoValues(), new SeoValues().jsonLdRaw("{}"), home).jsonLd()).isEmpty();
    }

    @Test
    void noRequestMeansNoCanonicalNoHreflangAndRelativeUrlsLeftOut() {
        SeoValues java = new SeoValues().image("/i.png").alternate(Locale.ENGLISH, "/en");
        ResolvedSeo seo = resolver().resolve(new SeoValues(), java, Optional.empty());
        assertThat(seo.canonical()).isEmpty();
        assertThat(seo.hreflang()).isEmpty();
        assertThat(seo.image()).isEmpty();
    }
}
