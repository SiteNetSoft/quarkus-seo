package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.seo.RobotsCustomizer;

class DefaultRobotsTxtProviderTest {

    private static final String BASE = "https://ex.com";
    private static final String SITEMAP = "\nSitemap: https://ex.com/sitemap.xml\n";

    private static final class Group implements SeoConfig.RobotsGroupConfig {
        private final List<String> agents;
        private final List<String> allow;
        private final List<String> disallow;
        private final Integer delay;

        Group(List<String> agents, List<String> allow, List<String> disallow, Integer delay) {
            this.agents = agents;
            this.allow = allow;
            this.disallow = disallow;
            this.delay = delay;
        }

        @Override
        public Optional<List<String>> userAgents() {
            return Optional.of(agents);
        }

        @Override
        public Optional<List<String>> allow() {
            return Optional.ofNullable(allow);
        }

        @Override
        public Optional<List<String>> disallow() {
            return Optional.ofNullable(disallow);
        }

        @Override
        public Optional<Integer> crawlDelay() {
            return Optional.ofNullable(delay);
        }
    }

    private static DefaultRobotsTxtProvider provider(TestConfig test, boolean indexable,
            RobotsCustomizer... customizers) {
        test.withDefault("sitemap.path", "/sitemap.xml");
        SeoSettings settings = new SeoSettings(Optional.empty(), indexable, null, Optional.empty(), "");
        return new DefaultRobotsTxtProvider(test.config(), test.buildConfig(), settings, List.of(customizers));
    }

    private static String agents(List<String> tokens) {
        StringBuilder text = new StringBuilder();
        for (String token : tokens) {
            text.append("User-agent: ").append(token).append('\n');
        }
        return text.toString();
    }

    @Test
    void noGroupsAllowsAll() {
        assertThat(provider(new TestConfig(), true).robotsTxt(BASE))
                .isEqualTo("User-agent: *\nAllow: /\n" + SITEMAP);
    }

    @Test
    void groupsSortedByNameWithAllFields() {
        TestConfig test = new TestConfig().with("robots.groups", Map.of(
                "b", new Group(List.of("Bingbot"), null, List.of("/x"), null),
                "a", new Group(List.of("*"), List.of("/"), List.of("/account", "/admin"), 5)));
        assertThat(provider(test, true).robotsTxt(BASE)).isEqualTo(
                "User-agent: *\nAllow: /\nDisallow: /account\nDisallow: /admin\nCrawl-delay: 5\n\n"
                        + "User-agent: Bingbot\nDisallow: /x\n" + SITEMAP);
    }

    @Test
    void blockTrainingAddsOneGroupWithTrainingAgents() {
        TestConfig test = new TestConfig().with("robots.ai", SeoConfig.AiPreset.BLOCK_TRAINING);
        assertThat(provider(test, true).robotsTxt(BASE)).isEqualTo("User-agent: *\nAllow: /\n\n"
                + agents(AiCrawlers.TRAINING) + "Disallow: /\n" + SITEMAP);
    }

    @Test
    void blockAllAddsTrainingAndSearchAgents() {
        TestConfig test = new TestConfig().with("robots.ai", SeoConfig.AiPreset.BLOCK_ALL);
        assertThat(provider(test, true).robotsTxt(BASE)).contains(
                agents(AiCrawlers.TRAINING) + agents(AiCrawlers.SEARCH) + "Disallow: /\n");
    }

    @Test
    void configuredAgentListsReplaceTheBuiltIns() {
        TestConfig test = new TestConfig().with("robots.ai", SeoConfig.AiPreset.BLOCK_ALL)
                .with("robots.aiAgents.training", List.of("T1"))
                .with("robots.aiAgents.search", List.of("S1"));
        assertThat(provider(test, true).robotsTxt(BASE)).contains("User-agent: T1\nUser-agent: S1\nDisallow: /\n")
                .doesNotContain("GPTBot");
    }

    @Test
    void notIndexableDisallowsEverythingAndHasNoSitemap() {
        boolean[] ran = { false };
        assertThat(provider(new TestConfig(), false, rules -> ran[0] = true).robotsTxt(BASE))
                .isEqualTo("User-agent: *\nDisallow: /\n");
        assertThat(ran[0]).isFalse();
    }

    @Test
    void noSitemapLineWhenSitemapDisabled() {
        TestConfig test = new TestConfig().with("sitemap.enabled", false);
        assertThat(provider(test, true).robotsTxt(BASE)).isEqualTo("User-agent: *\nAllow: /\n");
    }

    @Test
    void sitemapPathIsNormalized() {
        DefaultRobotsTxtProvider provider = provider(new TestConfig().with("sitemap.path", " maps/s.xml "), true);
        assertThat(provider.robotsTxt("https://ex.com/app")).endsWith("Sitemap: https://ex.com/app/maps/s.xml\n");
        DefaultRobotsTxtProvider slashes = provider(new TestConfig().with("sitemap.path", "//maps//s.xml/"), true);
        assertThat(slashes.robotsTxt("https://ex.com")).endsWith("Sitemap: https://ex.com/maps/s.xml\n");
    }

    @Test
    void customizerRunsAfterConfig() {
        RobotsCustomizer customizer = rules -> {
            rules.groups().get(0).disallow("/private");
            rules.sitemaps().add("https://ex.com/news.xml");
        };
        assertThat(provider(new TestConfig(), true, customizer).robotsTxt(BASE)).isEqualTo(
                "User-agent: *\nAllow: /\nDisallow: /private\n\n"
                        + "Sitemap: https://ex.com/sitemap.xml\nSitemap: https://ex.com/news.xml\n");
    }
}
