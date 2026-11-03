package org.sitenetsoft.quarkus.seo.deployment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.sitenetsoft.quarkus.seo.Seo;
import org.sitenetsoft.quarkus.seo.SeoCaches;
import org.sitenetsoft.quarkus.seo.runtime.DefaultBaseUrlResolver;
import org.sitenetsoft.quarkus.seo.runtime.DefaultRobotsTxtProvider;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSeoHeadRenderer;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSeoLocales;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSitemapWriter;
import org.sitenetsoft.quarkus.seo.runtime.NoSeoOutputCache;
import org.sitenetsoft.quarkus.seo.runtime.QuarkusLocaleKeys;
import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoLayers;
import org.sitenetsoft.quarkus.seo.runtime.SeoPaths;
import org.sitenetsoft.quarkus.seo.runtime.SeoRecorder;
import org.sitenetsoft.quarkus.seo.runtime.SeoResolver;
import org.sitenetsoft.quarkus.seo.runtime.SeoSectionHelperFactory;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;
import org.sitenetsoft.quarkus.seo.runtime.SeoTemplateExtensions;
import org.sitenetsoft.quarkus.seo.runtime.SitemapEntries;
import org.sitenetsoft.quarkus.seo.runtime.SitemapService;

/**
 * Register the SEO feature, its beans and its routes.
 */
class SeoProcessor {

    static final String FEATURE = "seo";
    private static final String CACHE_MANAGER_CLASS = "org.sitenetsoft.quarkus.seo.runtime.CacheManagerSeoOutputCache";
    private static final String ROBOTS_PATH = "/robots.txt";
    private static final String REST_PATH = "quarkus.rest.path";
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
     * Decide which outputs the extension registers.
     *
     * <p>
     * An output is off when the master switch or its feature switch is off. The sitemap and robots.txt are also off
     * when the application serves their path itself, as a static file, a JAX-RS resource or a Reactive Route. A
     * JAX-RS path includes the {@code @ApplicationPath} or {@code quarkus.rest.path} prefix. The step treats every
     * match on {@code /robots.txt} as owned, whatever the root path. The step logs nothing about owned paths.
     *
     * @param config the build-time configuration
     * @param index the combined index
     * @param statics the static resources, absent when the application has none
     * @return the outputs
     * @throws IllegalStateException when a sitemap path holds a colon, an asterisk or whitespace
     */
    @BuildStep
    SeoOutputsBuildItem outputs(SeoBuildConfig config, CombinedIndexBuildItem index,
            Optional<StaticResourcesBuildItem> statics) {
        if (!config.enabled()) {
            return new SeoOutputsBuildItem(false, false, false, Set.of());
        }
        if (config.sitemap().enabled()) {
            checkPath("quarkus.seo.sitemap.path", config.sitemap().path());
            checkPath("quarkus.seo.sitemap.page-path", config.sitemap().pagePath());
        }
        String sitemapPath = SeoPaths.normalize(config.sitemap().path());
        Set<String> appPaths = OwnedPaths.fromIndex(index.getIndex(), jaxrsPrefix(index));
        Set<String> owned = new LinkedHashSet<>();
        for (String path : List.of(sitemapPath, ROBOTS_PATH)) {
            if (appPaths.contains(path) || OwnedPaths.ownedStatic(statics, path)) {
                owned.add(path);
            }
        }
        boolean sitemap = config.sitemap().enabled() && !owned.contains(sitemapPath);
        boolean robots = config.robots().enabled() && !owned.contains(ROBOTS_PATH);
        return new SeoOutputsBuildItem(config.meta().enabled(), sitemap, robots, owned);
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
     * Register the page bean, the request layers, the resolver, the head renderer, the seo tag and the seo namespace.
     *
     * <p>
     * Qute reads the tag and the namespace from the bean archive index only, so both classes are added as beans.
     *
     * @param outputs the outputs that the extension registers
     * @return the beans, or null when the meta feature is switched off
     */
    @BuildStep
    AdditionalBeanBuildItem metaBeans(SeoOutputsBuildItem outputs) {
        if (!outputs.meta()) {
            return null;
        }
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(Seo.class, SeoLayers.class, SeoResolver.class, DefaultSeoHeadRenderer.class,
                        SeoSectionHelperFactory.class, SeoTemplateExtensions.class)
                .setUnremovable()
                .build();
    }

    /**
     * Register the filter that writes the {@code X-Robots-Tag} header.
     *
     * @param recorder the recorder
     * @param outputs the outputs that the extension registers
     * @return the filter at priority 100, or null when the meta feature is switched off
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    FilterBuildItem robotsHeaderFilter(SeoRecorder recorder, SeoOutputsBuildItem outputs) {
        if (!outputs.meta()) {
            return null;
        }
        return new FilterBuildItem(recorder.robotsHeaderFilter(), 100);
    }

    /**
     * Register the output cache and the invalidation bean.
     *
     * <p>
     * The cache-backed class is named as a string, so the class loader does not load it without
     * {@code quarkus-cache}. The run-time keys decide whether the bean uses its caches.
     *
     * @param outputs the outputs that the extension registers
     * @param capabilities the capabilities of the application
     * @param beans the producer of the beans
     */
    @BuildStep
    void cacheBeans(SeoOutputsBuildItem outputs, Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        if (!outputs.sitemap() && !outputs.robots()) {
            return;
        }
        beans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(SeoCaches.class).setUnremovable().build());
        String implementation = capabilities.isPresent(Capability.CACHE) ? CACHE_MANAGER_CLASS
                : NoSeoOutputCache.class.getName();
        beans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(implementation).setUnremovable().build());
    }

    /**
     * Index the cache-backed class so that the cache extension finds its {@code @CacheName} injection points.
     *
     * <p>
     * The step cannot consume the outputs, because the outputs read the combined index. It reads the build-time
     * switches instead, which are a superset of the outputs.
     *
     * @param config the build-time configuration
     * @param capabilities the capabilities of the application
     * @return the indexed class, or null when no cache is possible
     */
    @BuildStep
    AdditionalIndexedClassesBuildItem indexCacheClass(SeoBuildConfig config, Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.CACHE) || !config.enabled()
                || !(config.sitemap().enabled() || config.robots().enabled())) {
            return null;
        }
        return new AdditionalIndexedClassesBuildItem(CACHE_MANAGER_CLASS);
    }

    /**
     * Log once at start when a cache key is on and {@code quarkus-cache} is absent.
     *
     * @param recorder the recorder
     * @param outputs the outputs that the extension registers
     * @param capabilities the capabilities of the application
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void logCacheMissing(SeoRecorder recorder, SeoOutputsBuildItem outputs, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE) || (!outputs.sitemap() && !outputs.robots())) {
            return;
        }
        recorder.logCacheMissing(outputs.sitemap(), outputs.robots());
    }

    /**
     * Register the sitemap writer and the sitemap service.
     *
     * @param outputs the outputs that the extension registers
     * @return the beans, or null when the extension does not serve the sitemap
     */
    @BuildStep
    AdditionalBeanBuildItem sitemapBeans(SeoOutputsBuildItem outputs) {
        if (!outputs.sitemap()) {
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
     * @param outputs the outputs that the extension registers
     * @param index the combined index
     * @param entries the producer of the entries
     */
    @BuildStep
    void sitemapEntries(SeoOutputsBuildItem outputs, CombinedIndexBuildItem index,
            BuildProducer<SitemapEntryBuildItem> entries) {
        if (!outputs.sitemap()) {
            return;
        }
        for (SitemapEntryBuildItem entry : SitemapEntryScanner.scan(index.getIndex(), jaxrsPrefix(index))) {
            entries.produce(entry);
        }
    }

    /**
     * Record the sitemap entries as a bean.
     *
     * @param recorder the recorder
     * @param outputs the outputs that the extension registers
     * @param entries the entries found at build time
     * @return the synthetic bean, or null when the extension does not serve the sitemap
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem sitemapEntriesBean(SeoRecorder recorder, SeoOutputsBuildItem outputs,
            List<SitemapEntryBuildItem> entries) {
        if (!outputs.sitemap()) {
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
     * @param outputs the outputs that the extension registers
     * @param routes the producer of the routes
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void sitemapRoutes(SeoRecorder recorder, SeoBuildConfig config, SeoOutputsBuildItem outputs,
            BuildProducer<RouteBuildItem> routes) {
        if (!outputs.sitemap()) {
            return;
        }
        String path = SeoPaths.normalize(config.sitemap().path());
        String pageRegex = pageRegex(SeoPaths.normalize(config.sitemap().pagePath()));
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
     * Register the default robots.txt provider.
     *
     * @param outputs the outputs that the extension registers
     * @return the bean, or null when the extension does not serve robots.txt
     */
    @BuildStep
    AdditionalBeanBuildItem robotsBeans(SeoOutputsBuildItem outputs) {
        if (!outputs.robots()) {
            return null;
        }
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(DefaultRobotsTxtProvider.class)
                .setUnremovable()
                .build();
    }

    /**
     * Register the robots.txt route.
     *
     * <p>
     * The route sits on the host root unless {@code serve-under-root-path} is true. Both routes answer GET and HEAD
     * only.
     *
     * @param recorder the recorder
     * @param config the build-time configuration
     * @param outputs the outputs that the extension registers
     * @param nonApplicationRootPath the non-application root path build item
     * @param routes the producer of the routes
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void robotsRoute(SeoRecorder recorder, SeoBuildConfig config, SeoOutputsBuildItem outputs,
            NonApplicationRootPathBuildItem nonApplicationRootPath, BuildProducer<RouteBuildItem> routes) {
        if (!outputs.robots()) {
            return;
        }
        if (config.robots().serveUnderRootPath()) {
            routes.produce(RouteBuildItem.builder()
                    .routeFunction(recorder.getRoute(ROBOTS_PATH))
                    .handler(recorder.robotsHandler())
                    .handlerType(HandlerType.BLOCKING)
                    .build());
            return;
        }
        routes.produce(nonApplicationRootPath.routeBuilder()
                .routeFunction(ROBOTS_PATH, recorder.getAndHead())
                .handler(recorder.robotsHandler())
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

    /**
     * Check that a sitemap path is a plain path.
     *
     * <p>
     * Vert.x reads a colon as a path parameter and an asterisk as a wildcard.
     *
     * @param key the configuration key
     * @param value the configured path
     * @throws IllegalStateException when the path holds a colon, an asterisk or whitespace
     */
    static void checkPath(String key, String value) {
        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == ':' || c == '*' || Character.isWhitespace(c)) {
                throw new IllegalStateException(
                        key + " must not hold a colon, an asterisk or whitespace: \"" + value + "\"");
            }
        }
    }

    /**
     * Return the JAX-RS prefix of the application.
     *
     * @param index the combined index
     * @return the prefix, or an empty string for the root
     */
    private static String jaxrsPrefix(CombinedIndexBuildItem index) {
        Optional<String> restPath = ConfigProvider.getConfig().getOptionalValue(REST_PATH, String.class);
        return RestPaths.prefix(index.getIndex(), restPath);
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
