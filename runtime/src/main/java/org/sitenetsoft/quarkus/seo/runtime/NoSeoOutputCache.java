package org.sitenetsoft.quarkus.seo.runtime;

import java.util.function.Supplier;

import jakarta.inject.Singleton;

/**
 * Compute every output and store nothing.
 */
@Singleton
public class NoSeoOutputCache implements SeoOutputCache {

    /**
     * Compute the output.
     *
     * @param cacheName the cache name
     * @param key the key
     * @param compute the supplier of the output
     * @return the computed output
     */
    @Override
    public String get(String cacheName, String key, Supplier<String> compute) {
        return compute.get();
    }

    /**
     * Do nothing.
     *
     * @param cacheName the cache name
     */
    @Override
    public void invalidateAll(String cacheName) {
        // No cache holds entries.
    }
}
