package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

class LlmsTxtWriterTest {

    private static String write(Optional<String> description, SitemapUrl... urls) {
        return LlmsTxtWriter.write("Ex", description, List.of(urls), "https://ex.com");
    }

    @Test
    void headingQuoteAndLinks() {
        assertThat(LlmsTxtWriter.write("Ex", Optional.of("Shop for tools."),
                List.of(SitemapUrl.of("/a").title("A page"), SitemapUrl.of("/b")), "https://ex.com"))
                .isEqualTo("# Ex\n\n> Shop for tools.\n\n- [A page](https://ex.com/a)\n");
    }

    @Test
    void noDescriptionLeavesTheQuoteOut() {
        assertThat(write(Optional.empty(), SitemapUrl.of("/a").title("A")))
                .isEqualTo("# Ex\n\n- [A](https://ex.com/a)\n");
        assertThat(write(Optional.of("  "), SitemapUrl.of("/a").title("A")))
                .isEqualTo("# Ex\n\n- [A](https://ex.com/a)\n");
    }

    @Test
    void noTitledUrlLeavesOnlyTheHeading() {
        assertThat(write(Optional.empty(), SitemapUrl.of("/a"))).isEqualTo("# Ex\n");
    }

    @Test
    void markdownInTitlesIsEscaped() {
        assertThat(write(Optional.empty(), SitemapUrl.of("/a").title("a]b[c\\d<e")))
                .contains("- [a\\]b\\[c\\\\d&lt;e](https://ex.com/a)\n");
    }

    @Test
    void newlinesInTitleAndDescriptionBecomeSpaces() {
        assertThat(write(Optional.of("one\ntwo\r\nthree"), SitemapUrl.of("/a").title("x\ny\rz")))
                .isEqualTo("# Ex\n\n> one two three\n\n- [x y z](https://ex.com/a)\n");
    }

    @Test
    void parenthesisInUrlIsPercentEncoded() {
        assertThat(write(Optional.empty(), SitemapUrl.of("/a(b)").title("A")))
                .contains("(https://ex.com/a%28b%29)");
    }
}
