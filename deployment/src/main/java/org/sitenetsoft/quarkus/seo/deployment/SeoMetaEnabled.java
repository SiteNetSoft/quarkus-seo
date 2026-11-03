package org.sitenetsoft.quarkus.seo.deployment;

import java.util.function.BooleanSupplier;

import org.sitenetsoft.quarkus.seo.runtime.SeoBuildConfig;

/**
 * Tell whether the meta tag feature is registered, so that the extension provides its capability.
 */
public class SeoMetaEnabled implements BooleanSupplier {

    private final SeoBuildConfig config;

    /**
     * Create the condition.
     *
     * @param config the build-time configuration
     */
    public SeoMetaEnabled(SeoBuildConfig config) {
        this.config = config;
    }

    /**
     * Tell whether the master switch and the meta switch are both on.
     *
     * @return true when the {@code {#seo}} tag, the {@code seo} namespace and the {@code Seo} bean exist
     */
    @Override
    public boolean getAsBoolean() {
        return config.enabled() && config.meta().enabled();
    }
}
