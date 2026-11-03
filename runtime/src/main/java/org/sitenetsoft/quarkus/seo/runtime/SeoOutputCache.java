package org.sitenetsoft.quarkus.seo.runtime;

import java.util.function.Supplier;

/**
 * Store generated text outputs by name and key.
 *
 * <p>
 * The cache names are {@code seo-sitemap} and {@code seo-robots}.
 */
public interface SeoOutputCache {

    /**
     * The name of the sitemap cache.
     */
    String SITEMAP = "seo-sitemap";

    /**
     * The name of the robots.txt cache.
     */
    String ROBOTS = "seo-robots";

    /**
     * Return the cached output, or compute and store it.
     *
     * <p>
     * A computation that throws stores nothing.
     *
     * @param cacheName the cache name
     * @param key the key, which holds the base URL
     * @param compute the supplier of the output
     * @return the output
     */
    String get(String cacheName, String key, Supplier<String> compute);

    /**
     * Remove every entry of a cache.
     *
     * @param cacheName the cache name
     */
    void invalidateAll(String cacheName);
}
