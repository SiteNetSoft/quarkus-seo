package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Image;
import org.sitenetsoft.quarkus.seo.ResolvedSeo.Link;

class SeoChecksTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    private static ResolvedSeo seo(String title, String description, String image) {
        return new ResolvedSeo(title, Optional.ofNullable(description), List.of(), Optional.empty(), List.of(),
                "website", Optional.of(title), Optional.empty(), "en_US", List.of(),
                Optional.ofNullable(image).map(url -> new Image(url, null, null, null)), Optional.empty(), "summary",
                Optional.empty(), Optional.empty(), List.of(), Optional.empty(), List.of(), "", Optional.empty());
    }

    private static List<String> codes(List<SeoWarning> warnings) {
        return warnings.stream().map(SeoWarning::code).toList();
    }

    private static List<String> expiry(String value) {
        TestConfig test = new TestConfig();
        if (value != null) {
            test.with("securityTxt.expires", value);
        }
        return codes(SeoChecks.securityTxt(test.config().securityTxt(), NOW));
    }

    @Test
    void cleanPageHasNoWarnings() {
        assertThat(SeoChecks.page(seo("t".repeat(30), "d".repeat(100), "https://ex.com/i.png"))).isEmpty();
    }

    @Test
    void missingTitleAndDescription() {
        assertThat(codes(SeoChecks.page(seo("", null, "https://ex.com/i.png"))))
                .containsExactly("missing-title", "missing-description");
    }

    @Test
    void titleOver60Characters() {
        assertThat(codes(SeoChecks.page(seo("t".repeat(61), "d".repeat(100), "https://ex.com/i.png"))))
                .containsExactly("long-title");
        assertThat(SeoChecks.page(seo("t".repeat(60), "d".repeat(100), "https://ex.com/i.png"))).isEmpty();
    }

    @Test
    void descriptionOutside50To160() {
        for (int length : new int[] { 49, 161 }) {
            assertThat(codes(SeoChecks.page(seo("Title", "d".repeat(length), "https://ex.com/i.png"))))
                    .containsExactly("description-length");
        }
        for (int length : new int[] { 50, 160 }) {
            assertThat(SeoChecks.page(seo("Title", "d".repeat(length), "https://ex.com/i.png"))).isEmpty();
        }
    }

    @Test
    void missingAndRelativeImage() {
        assertThat(codes(SeoChecks.page(seo("Title", "d".repeat(100), null)))).containsExactly("missing-image");
        assertThat(codes(SeoChecks.page(seo("Title", "d".repeat(100), "/i.png")))).containsExactly("relative-image");
    }

    @Test
    void hreflangWithoutReturnLink() {
        List<Link> links = List.of(new Link("en", "https://ex.com/a"), new Link("fr", "https://ex.com/b"),
                new Link("x-default", "https://ex.com/c"));
        Map<String, List<Link>> alternates = Map.of("https://ex.com/b", List.of(new Link("fr", "https://ex.com/b")),
                "https://ex.com/c", List.of());
        List<SeoWarning> warnings = SeoChecks.oneWayHreflang("https://ex.com/a", links, alternates);
        assertThat(codes(warnings)).containsExactly("one-way-hreflang");
        assertThat(warnings.get(0).message()).contains("https://ex.com/b");
    }

    @Test
    void hreflangWithReturnLinkIsClean() {
        List<Link> links = List.of(new Link("fr", "https://ex.com/b"));
        Map<String, List<Link>> alternates = Map.of("https://ex.com/b", List.of(new Link("en", "https://ex.com/a")));
        assertThat(SeoChecks.oneWayHreflang("https://ex.com/a", links, alternates)).isEmpty();
    }

    @Test
    void hreflangNotFetchedIsNotReported() {
        List<Link> links = List.of(new Link("fr", "https://ex.com/b"));
        assertThat(SeoChecks.oneWayHreflang("https://ex.com/a", links, Map.of())).isEmpty();
    }

    @Test
    void duplicateTitlesNameEveryUrl() {
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put("/a", "Home");
        titles.put("/b", "Home");
        titles.put("/c", "About");
        List<SeoWarning> warnings = SeoChecks.duplicateTitles(titles);
        assertThat(codes(warnings)).containsExactly("duplicate-title");
        assertThat(warnings.get(0).message()).contains("/a", "/b").doesNotContain("/c");
    }

    @Test
    void securityTxtExpiry() {
        assertThat(expiry("2027-01-20")).containsExactly("security-txt-expires-soon");
        assertThat(expiry("2026-12-31")).containsExactly("security-txt-expired");
        assertThat(expiry("2027-01-01")).containsExactly("security-txt-expired");
        assertThat(expiry("2027-03-01")).isEmpty();
        assertThat(expiry(null)).containsExactly("security-txt-expires-missing");
        assertThat(expiry("soon")).containsExactly("security-txt-expires-invalid");
    }
}
