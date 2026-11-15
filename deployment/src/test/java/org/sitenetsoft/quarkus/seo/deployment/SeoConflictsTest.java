package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeoConflictsTest {

    @TempDir
    Path root;

    @Test
    void findsATagWithAnySuffix() throws IOException {
        assertThat(SeoConflicts.hasSeoTag(loader("templates/tags/seo.html"))).isTrue();
        assertThat(SeoConflicts.hasSeoTag(loader("templates/tags/seo.txt"))).isTrue();
        assertThat(SeoConflicts.hasSeoTag(loader("templates/tags/seo.qute.html"))).isTrue();
    }

    @Test
    void ignoresOtherTags() throws IOException {
        assertThat(SeoConflicts.hasSeoTag(loader("templates/tags/seoTitle.html"))).isFalse();
        assertThat(SeoConflicts.hasSeoTag(loader("templates/seo.html"))).isFalse();
    }

    @Test
    void excludesOnlyTheSeoTag() {
        Pattern tag = Pattern.compile(SeoConflicts.SEO_TAG_PATHS);
        assertThat(tag.matcher("tags/seo.html").matches()).isTrue();
        assertThat(tag.matcher("tags/seo").matches()).isTrue();
        assertThat(tag.matcher("tags/seoTitle.html").matches()).isFalse();
        assertThat(tag.matcher("seo.html").matches()).isFalse();
    }

    @Test
    void findsRoqSitemapPlugin() throws IOException {
        assertThat(SeoConflicts.hasRoqSitemap(loader(SeoConflicts.ROQ_SITEMAP_CLASS))).isTrue();
        assertThat(SeoConflicts.hasRoqSitemap(loader("io/quarkiverse/roq/frontmatter/runtime/model/Site.class")))
                .isFalse();
    }

    private ClassLoader loader(String resource) throws IOException {
        Path dir = Files.createTempDirectory(root, "cp");
        Path file = dir.resolve(resource);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "x");
        return new URLClassLoader(new URL[] { dir.toUri().toURL() }, null);
    }
}
