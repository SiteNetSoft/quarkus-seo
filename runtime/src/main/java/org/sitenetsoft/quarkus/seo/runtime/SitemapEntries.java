package org.sitenetsoft.quarkus.seo.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.sitenetsoft.quarkus.seo.ChangeFrequency;
import org.sitenetsoft.quarkus.seo.SitemapUrl;

/**
 * Hold the sitemap entries that the build found on annotated JAX-RS methods.
 */
public final class SitemapEntries {

    private final List<String> paths;
    private final List<String> changefreqs;
    private final List<Double> priorities;

    /**
     * Create the holder from three lists of the same length.
     *
     * @param paths the paths of the entries
     * @param changefreqs the lower case change frequencies, with an empty string when unset
     * @param priorities the priorities, with -1 when unset
     */
    public SitemapEntries(List<String> paths, List<String> changefreqs, List<Double> priorities) {
        if (paths.size() != changefreqs.size() || paths.size() != priorities.size()) {
            throw new IllegalArgumentException("The sitemap entry lists must have the same length");
        }
        this.paths = List.copyOf(paths);
        this.changefreqs = List.copyOf(changefreqs);
        this.priorities = List.copyOf(priorities);
    }

    /**
     * Build a new sitemap URL for each entry.
     *
     * @return the URLs in build order
     */
    public List<SitemapUrl> urls() {
        List<SitemapUrl> urls = new ArrayList<>(paths.size());
        for (int i = 0; i < paths.size(); i++) {
            SitemapUrl url = SitemapUrl.of(paths.get(i));
            String changefreq = changefreqs.get(i);
            if (!changefreq.isEmpty()) {
                url.changefreq(ChangeFrequency.valueOf(changefreq.toUpperCase(Locale.ROOT)));
            }
            double priority = priorities.get(i);
            if (priority >= 0) {
                url.priority(priority);
            }
            urls.add(url);
        }
        return urls;
    }
}
