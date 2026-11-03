package org.sitenetsoft.quarkus.seo.deployment;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.sitenetsoft.quarkus.seo.Seo;
import org.sitenetsoft.quarkus.seo.runtime.DefaultBaseUrlResolver;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSeoHeadRenderer;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSeoLocales;
import org.sitenetsoft.quarkus.seo.runtime.QuarkusLocaleKeys;
import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoRecorder;
import org.sitenetsoft.quarkus.seo.runtime.SeoResolver;
import org.sitenetsoft.quarkus.seo.runtime.SeoSectionHelperFactory;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;
import org.sitenetsoft.quarkus.seo.runtime.SeoTemplateExtensions;

/**
 * Registers the SEO feature, its beans and, in later tasks, its routes.
 */
class SeoProcessor {

    static final String FEATURE = "seo";

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
