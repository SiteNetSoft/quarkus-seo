package org.sitenetsoft.quarkus.seo.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

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
}
