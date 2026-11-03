package org.sitenetsoft.quarkus.seo.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.sitenetsoft.quarkus.seo.Seo;
import org.sitenetsoft.quarkus.seo.runtime.DefaultBaseUrlResolver;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSeoHeadRenderer;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSeoLocales;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSitemapWriter;
import org.sitenetsoft.quarkus.seo.runtime.QuarkusLocaleKeys;
import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoRecorder;
import org.sitenetsoft.quarkus.seo.runtime.SeoResolver;
import org.sitenetsoft.quarkus.seo.runtime.SeoSectionHelperFactory;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;
import org.sitenetsoft.quarkus.seo.runtime.SeoTemplateExtensions;
import org.sitenetsoft.quarkus.seo.runtime.SitemapEntries;
import org.sitenetsoft.quarkus.seo.runtime.SitemapService;

/**
 * Registers the SEO feature, its beans and, in later tasks, its routes.
 */
class SeoProcessor {

    static final String FEATURE = "seo";
    private static final String PAGE_TOKEN = "{n}";
    private static final String PAGE_GROUP = "([1-9][0-9]*)";

    /**
     * Announce the SEO feature to Quarkus.
     *
     * @return the build item that installs the feature
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Register the settings, locale and base URL beans.
     *
     * @param config the build-time configuration
     * @return the beans, or null when the extension is switched off
     */
    @BuildStep
    AdditionalBeanBuildItem coreBeans(SeoBuildConfig config) {
        if (!config.enabled()) {
            return null;
        }
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(SeoSettings.class, DefaultSeoLocales.class, DefaultBaseUrlResolver.class)
                .setUnremovable()
                .build();
    }

    /**
     * Register the page bean, the resolver, the head renderer, the seo tag and the seo namespace.
     *
     * <p>
     * Qute reads the tag and the namespace from the bean archive index only, so both classes are added as beans.
     *
     * @param config the build-time configuration
     * @return the beans, or null when the meta feature is switched off
     */
    @BuildStep
    AdditionalBeanBuildItem metaBeans(SeoBuildConfig config) {
        if (!metaEnabled(config)) {
            return null;
        }
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(Seo.class, SeoResolver.class, DefaultSeoHeadRenderer.class,
                        SeoSectionHelperFactory.class, SeoTemplateExtensions.class)
                .setUnremovable()
                .build();
    }

    /**
     * Tell whether the meta feature is switched on.
     *
     * @param config the build-time configuration
     * @return true when the extension and the meta feature are switched on
     */
    static boolean metaEnabled(SeoBuildConfig config) {
        return config.enabled() && config.meta().enabled();
    }

    /**
     * Register the filter that writes the {@code X-Robots-Tag} header.
     *
     * @param recorder the recorder
     * @param config the build-time configuration
     * @return the filter at priority 100, or null when the meta feature is switched off
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    FilterBuildItem robotsHeaderFilter(SeoRecorder recorder, SeoBuildConfig config) {
        if (!metaEnabled(config)) {
            return null;
        }
        return new FilterBuildItem(recorder.robotsHeaderFilter(), 100);
    }

    /**
     * Tell whether the sitemap feature is switched on.
     *
     * @param config the build-time configuration
     * @return true when the extension and the sitemap feature are switched on
     */
    static boolean sitemapEnabled(SeoBuildConfig config) {
        return config.enabled() && config.sitemap().enabled();
    }

    /**
     * Register the sitemap writer and the sitemap service.
     *
     * @param config the build-time configuration
     * @return the beans, or null when the sitemap feature is switched off
     */
    @BuildStep
    AdditionalBeanBuildItem sitemapBeans(SeoBuildConfig config) {
        if (!sitemapEnabled(config)) {
            return null;
        }
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(DefaultSitemapWriter.class, SitemapService.class)
                .setUnremovable()
                .build();
    }

    /**
     * Find the JAX-RS methods annotated with {@code @SitemapEntry}.
     *
     * @param config the build-time configuration
     * @param index the combined index
     * @param entries the producer of the entries
     */
    @BuildStep
    void sitemapEntries(SeoBuildConfig config, CombinedIndexBuildItem index,
            BuildProducer<SitemapEntryBuildItem> entries) {
        if (!sitemapEnabled(config)) {
            return;
        }
        for (SitemapEntryBuildItem entry : SitemapEntryScanner.scan(index.getIndex())) {
            entries.produce(entry);
        }
    }

    /**
     * Record the sitemap entries as a bean.
     *
     * @param recorder the recorder
     * @param config the build-time configuration
     * @param entries the entries found at build time
     * @return the synthetic bean, or null when the sitemap feature is switched off
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem sitemapEntriesBean(SeoRecorder recorder, SeoBuildConfig config,
            List<SitemapEntryBuildItem> entries) {
        if (!sitemapEnabled(config)) {
            return null;
        }
        List<String> paths = new ArrayList<>();
        List<String> changefreqs = new ArrayList<>();
        List<Double> priorities = new ArrayList<>();
        for (SitemapEntryBuildItem entry : entries) {
            paths.add(entry.path());
            changefreqs.add(entry.changefreq());
            priorities.add(entry.priority());
        }
        return SyntheticBeanBuildItem.configure(SitemapEntries.class)
                .scope(Singleton.class)
                .unremovable()
                .supplier(recorder.sitemapEntries(paths, changefreqs, priorities))
                .done();
    }

    /**
     * Register the sitemap route and the sitemap page route under the HTTP root path.
     *
     * @param recorder the recorder
     * @param config the build-time configuration
     * @param routes the producer of the routes
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void sitemapRoutes(SeoRecorder recorder, SeoBuildConfig config, BuildProducer<RouteBuildItem> routes) {
        if (!sitemapEnabled(config)) {
            return;
        }
        String path = leadingSlash(config.sitemap().path());
        String pageRegex = pageRegex(leadingSlash(config.sitemap().pagePath()));
        routes.produce(RouteBuildItem.builder()
                .routeFunction(recorder.getRoute(path))
                .handler(recorder.sitemapHandler())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(RouteBuildItem.builder()
                .routeFunction(recorder.getRegexRoute(pageRegex))
                .handler(recorder.sitemapPageHandler())
                .handlerType(HandlerType.BLOCKING)
                .build());
    }

    /**
     * Turn the page path pattern into a regular expression.
     *
     * @param pagePath the page path with one {@code {n}} token
     * @return the regular expression, where the first group captures the page number
     * @throws IllegalStateException when the pattern does not hold exactly one {@code {n}} token
     */
    static String pageRegex(String pagePath) {
        int token = pagePath.indexOf(PAGE_TOKEN);
        if (token < 0 || pagePath.indexOf(PAGE_TOKEN, token + 1) >= 0) {
            throw new IllegalStateException(
                    "quarkus.seo.sitemap.page-path must hold the token {n} exactly once: " + pagePath);
        }
        return Pattern.quote(pagePath.substring(0, token)) + PAGE_GROUP
                + Pattern.quote(pagePath.substring(token + PAGE_TOKEN.length()));
    }

    private static String leadingSlash(String path) {
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    /**
     * Record the Quarkus locale keys that the application set.
     *
     * <p>
     * The build sees every config source. Quarkus fills {@code quarkus.locales} with the build machine locale when
     * no source sets it, so the step records nothing for that case.
     *
     * @param recorder the recorder
     * @param config the build-time configuration
     * @return the synthetic bean, or null when the extension is switched off
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem quarkusLocaleKeys(SeoRecorder recorder, SeoBuildConfig config) {
        if (!config.enabled()) {
            return null;
        }
        SmallRyeConfig buildConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        List<String> locales = new ArrayList<>();
        String locale = setValue(buildConfig, "quarkus.locales");
        if (locale != null) {
            for (String part : locale.split(",")) {
                if (!part.isBlank()) {
                    locales.add(part.trim());
                }
            }
        }
        String defaultLocale = setValue(buildConfig, "quarkus.default-locale");
        return SyntheticBeanBuildItem.configure(QuarkusLocaleKeys.class)
                .scope(Singleton.class)
                .unremovable()
                .supplier(recorder.localeKeys(locales, defaultLocale == null ? "" : defaultLocale))
                .done();
    }

    /**
     * Return the value of a key, or null when only the Quarkus default provides it.
     *
     * <p>
     * The raw value of the Quarkus default is an expression that reads the system locale properties.
     */
    private static String setValue(SmallRyeConfig config, String key) {
        ConfigValue value = config.getConfigValue(key);
        if (value == null || value.getValue() == null || value.getValue().isBlank()) {
            return null;
        }
        String raw = value.getRawValue();
        if (raw != null && raw.contains("${user.language")) {
            return null;
        }
        return value.getValue();
    }
}
