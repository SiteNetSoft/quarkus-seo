package org.sitenetsoft.quarkus.seo.it;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.quarkus.seo.SitemapContributor;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

/**
 * Contribute two blog URLs to the sitemap and count how often the sitemap asks for them.
 */
@ApplicationScoped
public class BlogContributor implements SitemapContributor {

    private final AtomicInteger calls = new AtomicInteger();

    /**
     * Return the blog URLs and count the call.
     *
     * @return the two blog URLs
     */
    @Override
    public Stream<SitemapUrl> urls() {
        calls.incrementAndGet();
        return Stream.of(
                SitemapUrl.of("/blog/first").title("First post").lastmod(Instant.parse("2026-01-15T10:00:00Z")),
                SitemapUrl.of("/blog/second").title("Second post").lastmod(Instant.parse("2026-02-20T10:00:00Z")));
    }

    /**
     * Return how many times the sitemap has asked for the URLs.
     *
     * @return the call count
     */
    public int calls() {
        return calls.get();
    }
}
