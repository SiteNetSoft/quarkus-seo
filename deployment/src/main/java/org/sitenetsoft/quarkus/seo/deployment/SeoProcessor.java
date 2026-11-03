package org.sitenetsoft.quarkus.seo.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.sitenetsoft.quarkus.seo.runtime.DefaultBaseUrlResolver;
import org.sitenetsoft.quarkus.seo.runtime.DefaultSeoLocales;
import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;

/**
 * Registers the SEO feature and, in later tasks, its beans and routes.
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
}
