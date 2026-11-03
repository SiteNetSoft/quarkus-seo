package org.sitenetsoft.quarkus.seo.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import org.sitenetsoft.quarkus.seo.RobotsCustomizer;
import org.sitenetsoft.quarkus.seo.RobotsGroup;
import org.sitenetsoft.quarkus.seo.RobotsRules;
import org.sitenetsoft.quarkus.seo.RobotsTxtProvider;

/**
 * Write robots.txt from the configured groups, the AI preset and the customizer beans.
 */
@DefaultBean
@Singleton
public class DefaultRobotsTxtProvider implements RobotsTxtProvider {

    private static final String DISALLOW_ALL = "User-agent: *\nDisallow: /\n";

    private final SeoConfig config;
    private final SeoBuildConfig buildConfig;
    private final SeoSettings settings;
    private final List<RobotsCustomizer> customizers;

    /**
     * Create the provider.
     *
     * @param config the run-time SEO configuration
     * @param buildConfig the build-time SEO configuration
     * @param settings the resolved site settings
     * @param customizers the customizer beans
     */
    @Inject
    public DefaultRobotsTxtProvider(SeoConfig config, SeoBuildConfig buildConfig, SeoSettings settings,
            @All List<RobotsCustomizer> customizers) {
        this.config = config;
        this.buildConfig = buildConfig;
        this.settings = settings;
        this.customizers = customizers;
    }

    @Override
    public String robotsTxt(String baseUrl) {
        if (!settings.indexable()) {
            return DISALLOW_ALL;
        }
        RobotsRules rules = new RobotsRules();
        addConfiguredGroups(rules);
        addAiGroups(rules);
        if (buildConfig.enabled() && buildConfig.sitemap().enabled()) {
            rules.sitemaps().add(Urls.absolute(baseUrl, SeoPaths.normalize(buildConfig.sitemap().path())));
        }
        for (RobotsCustomizer customizer : customizers) {
            customizer.customize(rules);
        }
        return write(rules);
    }

    private void addConfiguredGroups(RobotsRules rules) {
        Map<String, SeoConfig.RobotsGroupConfig> sorted = new TreeMap<>(config.robots().groups());
        for (SeoConfig.RobotsGroupConfig group : sorted.values()) {
            RobotsGroup built = new RobotsGroup(group.userAgents().orElse(List.of("*")));
            built.allow().addAll(group.allow().orElse(List.of()));
            built.disallow().addAll(group.disallow().orElse(List.of()));
            group.crawlDelay().ifPresent(built::crawlDelay);
            rules.groups().add(built);
        }
        if (rules.groups().isEmpty()) {
            rules.groups().add(new RobotsGroup(List.of("*")).allow("/"));
        }
    }

    private void addAiGroups(RobotsRules rules) {
        SeoConfig.AiPreset preset = config.robots().ai();
        if (preset == SeoConfig.AiPreset.ALLOW) {
            return;
        }
        List<String> agents = new ArrayList<>(config.robots().aiAgents().training().orElse(AiCrawlers.TRAINING));
        if (preset == SeoConfig.AiPreset.BLOCK_ALL) {
            agents.addAll(config.robots().aiAgents().search().orElse(AiCrawlers.SEARCH));
        }
        if (!agents.isEmpty()) {
            rules.groups().add(new RobotsGroup(agents).disallow("/"));
        }
    }

    private static String write(RobotsRules rules) {
        StringBuilder text = new StringBuilder();
        boolean first = true;
        for (RobotsGroup group : rules.groups()) {
            if (!first) {
                text.append('\n');
            }
            first = false;
            for (String agent : group.userAgents()) {
                text.append("User-agent: ").append(agent).append('\n');
            }
            for (String path : group.allow()) {
                text.append("Allow: ").append(path).append('\n');
            }
            for (String path : group.disallow()) {
                text.append("Disallow: ").append(path).append('\n');
            }
            if (group.crawlDelay().isPresent()) {
                text.append("Crawl-delay: ").append(group.crawlDelay().getAsInt()).append('\n');
            }
        }
        if (!rules.sitemaps().isEmpty()) {
            if (!first) {
                text.append('\n');
            }
            for (String sitemap : rules.sitemaps()) {
                text.append("Sitemap: ").append(sitemap).append('\n');
            }
        }
        return text.toString();
    }
}
