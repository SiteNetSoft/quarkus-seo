package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ManifestWriterTest {

    private static SeoConfig.Manifest cfg(TestConfig test) {
        return test.config().manifest();
    }

    private static SeoConfig.Icon icon(String src, String sizes, String type) {
        return new SeoConfig.Icon() {
            @Override
            public Optional<String> src() {
                return Optional.ofNullable(src);
            }

            @Override
            public Optional<String> sizes() {
                return Optional.ofNullable(sizes);
            }

            @Override
            public Optional<String> type() {
                return Optional.ofNullable(type);
            }
        };
    }

    @Test
    void fullManifestInKeyOrder() {
        TestConfig test = new TestConfig()
                .with("manifest.name", "Ex")
                .with("manifest.shortName", "E")
                .with("manifest.startUrl", "/start")
                .with("manifest.display", SeoConfig.Display.MINIMAL_UI)
                .with("manifest.themeColor", "#123")
                .with("manifest.backgroundColor", "#fff")
                .with("manifest.icons", Map.of("a", icon("/i/192.png", "192x192", "image/png")));
        assertThat(ManifestWriter.write(cfg(test), Optional.empty(), Optional.empty(), "/app"))
                .isEqualTo("{\"name\":\"Ex\",\"short_name\":\"E\",\"start_url\":\"/app/start\","
                        + "\"display\":\"minimal-ui\",\"theme_color\":\"#123\",\"background_color\":\"#fff\","
                        + "\"icons\":[{\"src\":\"/app/i/192.png\",\"sizes\":\"192x192\",\"type\":\"image/png\"}]}");
    }

    @Test
    void nameFallsBackToSiteNameAndThemeColorToTheSiteKey() {
        String json = ManifestWriter.write(cfg(new TestConfig()), Optional.of("Site"), Optional.of("#abc"), "");
        assertThat(json).contains("\"name\":\"Site\"").contains("\"theme_color\":\"#abc\"");
    }

    @Test
    void startUrlDefaultsToTheRootPath() {
        assertThat(ManifestWriter.write(cfg(new TestConfig()), Optional.of("Site"), Optional.empty(), "/app"))
                .contains("\"start_url\":\"/app/\"");
        assertThat(ManifestWriter.write(cfg(new TestConfig()), Optional.of("Site"), Optional.empty(), ""))
                .contains("\"start_url\":\"/\"");
    }

    @Test
    void absoluteIconUrlIsKept() {
        TestConfig test = new TestConfig().with("manifest.icons",
                Map.of("a", icon("https://cdn.ex.com/i.png", null, null)));
        assertThat(ManifestWriter.write(cfg(test), Optional.empty(), Optional.empty(), "/app"))
                .contains("\"icons\":[{\"src\":\"https://cdn.ex.com/i.png\"}]");
    }

    @Test
    void iconsSortedByName() {
        Map<String, SeoConfig.Icon> icons = new LinkedHashMap<>();
        icons.put("b", icon("/b.png", null, null));
        icons.put("a", icon("/a.png", null, null));
        String json = ManifestWriter.write(cfg(new TestConfig().with("manifest.icons", icons)), Optional.empty(),
                Optional.empty(), "");
        assertThat(json).contains("\"icons\":[{\"src\":\"/a.png\"},{\"src\":\"/b.png\"}]");
    }

    @Test
    void valuesAreJsonEscaped() {
        TestConfig test = new TestConfig().with("manifest.name", "a\"b\\c</script>\u0001");
        String json = ManifestWriter.write(cfg(test), Optional.empty(), Optional.empty(), "");
        assertThat(json).contains("\"name\":\"a\\\"b\\\\c\\u003c/script>\\u0001\"").doesNotContain("</script>");
    }

    @Test
    void emptyKeysAreLeftOut() {
        String json = ManifestWriter.write(cfg(new TestConfig()), Optional.empty(), Optional.empty(), "");
        assertThat(json).isEqualTo("{\"start_url\":\"/\"}");
    }

    @Test
    void iconWithoutSrcIsSkipped() {
        Map<String, SeoConfig.Icon> icons = new LinkedHashMap<>();
        icons.put("a", icon(null, "192x192", null));
        icons.put("b", icon("/b.png", null, null));
        icons.put("c", icon("  ", "64x64", null));
        assertThat(ManifestWriter.write(cfg(new TestConfig().with("manifest.icons", icons)), Optional.empty(),
                Optional.empty(), ""))
                .contains("\"icons\":[{\"src\":\"/b.png\"}]")
                .doesNotContain("192x192")
                .doesNotContain("64x64");
    }

    @Test
    void iconsKeyIsLeftOutWhenNoIconHasSrc() {
        TestConfig test = new TestConfig().with("manifest.icons", Map.of("a", icon(null, "192x192", null)));
        assertThat(ManifestWriter.write(cfg(test), Optional.empty(), Optional.empty(), ""))
                .doesNotContain("icons");
    }

    @Test
    void blankStartUrlCountsAsUnset() {
        TestConfig test = new TestConfig().with("manifest.startUrl", "  ");
        assertThat(ManifestWriter.write(cfg(test), Optional.empty(), Optional.empty(), "/app"))
                .contains("\"start_url\":\"/app/\"");
    }
}
