package org.sitenetsoft.quarkus.seo;

import java.util.List;
import java.util.Locale;

/**
 * Write sitemap documents.
 */
public interface SitemapWriter {

    /**
     * Write a sitemap with the given URLs.
     *
     * @param urls the URLs to list
     * @param baseUrl the base URL that makes paths absolute
     * @param defaultLocale the locale that receives the x-default alternate
     * @return the sitemap XML
     */
    String urlSet(List<SitemapUrl> urls, String baseUrl, Locale defaultLocale);

    /**
     * Write a sitemap index.
     *
     * @param pageUrls the absolute URLs of the sitemap pages
     * @return the sitemap index XML
     */
    String index(List<String> pageUrls);
}
