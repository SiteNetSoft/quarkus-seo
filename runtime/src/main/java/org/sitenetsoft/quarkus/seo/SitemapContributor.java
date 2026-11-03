package org.sitenetsoft.quarkus.seo;

import java.util.stream.Stream;

/**
 * Add URLs to the sitemap.
 */
public interface SitemapContributor {

    /**
     * List the URLs that this contributor adds.
     *
     * @return the URLs, as paths or absolute URLs
     */
    Stream<SitemapUrl> urls();
}
