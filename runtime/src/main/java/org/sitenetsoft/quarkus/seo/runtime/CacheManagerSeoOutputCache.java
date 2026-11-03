package org.sitenetsoft.quarkus.seo.runtime;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheException;
import io.quarkus.cache.CacheName;

/**
 * Store the outputs in Quarkus caches.
 *
 * <p>
 * The class loads only when {@code quarkus-cache} is present. A cache whose run-time key is off computes directly.
 */
@Singleton
public class CacheManagerSeoOutputCache implements SeoOutputCache {

    private final Cache sitemap;
    private final Cache robots;
    private final SeoConfig config;

    /**
     * Create the cache.
     *
     * @param sitemap the sitemap cache
     * @param robots the robots.txt cache
     * @param config the run-time SEO configuration
     */
    @Inject
    public CacheManagerSeoOutputCache(@CacheName(SeoOutputCache.SITEMAP) Cache sitemap,
            @CacheName(SeoOutputCache.ROBOTS) Cache robots, SeoConfig config) {
        this.sitemap = sitemap;
        this.robots = robots;
        this.config = config;
    }

    /**
     * Return the cached output, or compute and store it.
     *
     * <p>
     * The method blocks, so callers run it on a worker thread. It rethrows the failure of the computation as is.
     *
     * @param cacheName the cache name
     * @param key the key, which holds the base URL
     * @param compute the supplier of the output
     * @return the output
     */
    @Override
    public String get(String cacheName, String key, Supplier<String> compute) {
        if (!enabled(cacheName)) {
            return compute.get();
        }
        try {
            return cache(cacheName).get(key, k -> compute.get()).await().indefinitely();
        } catch (CacheException | CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    /**
     * Remove every entry of a cache.
     *
     * @param cacheName the cache name
     */
    @Override
    public void invalidateAll(String cacheName) {
        cache(cacheName).invalidateAll().await().indefinitely();
    }

    private boolean enabled(String cacheName) {
        if (SITEMAP.equals(cacheName)) {
            return config.sitemap().cache().enabled();
        }
        return config.robots().cache().enabled();
    }

    private Cache cache(String cacheName) {
        return SITEMAP.equals(cacheName) ? sitemap : robots;
    }
}
