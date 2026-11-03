package org.sitenetsoft.quarkus.seo;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.seo.runtime.SeoOutputCache;

/**
 * Invalidate the cached sitemap and robots.txt.
 *
 * <p>
 * The methods do nothing when no cache is active. They do not block and are safe on any thread, including a
 * Vert.x event loop. They never throw.
 */
@Singleton
public class SeoCaches {

    private final SeoOutputCache cache;

    /**
     * Create the bean.
     *
     * @param cache the output cache
     */
    @Inject
    SeoCaches(SeoOutputCache cache) {
        this.cache = cache;
    }

    /**
     * Remove every cached sitemap document.
     */
    public void invalidateSitemap() {
        cache.invalidateAll(SeoOutputCache.SITEMAP);
    }

    /**
     * Remove every cached robots.txt.
     */
    public void invalidateRobots() {
        cache.invalidateAll(SeoOutputCache.ROBOTS);
    }
}
