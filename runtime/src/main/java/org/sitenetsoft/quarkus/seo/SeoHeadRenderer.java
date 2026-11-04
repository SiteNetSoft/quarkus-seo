package org.sitenetsoft.quarkus.seo;

/**
 * Turn the resolved values of a page into head tags.
 */
public interface SeoHeadRenderer {

    /**
     * Render the head tags of one page.
     *
     * @param seo the resolved values
     * @return the head tags, one per line
     */
    String render(ResolvedSeo seo);
}
