package org.sitenetsoft.quarkus.seo.runtime;

import jakarta.enterprise.context.RequestScoped;

/**
 * Hold the tag layer and the Java layer of the current request.
 *
 * <p>
 * The {@code {#seo}} tag writes the tag layer and the {@code Seo} bean writes the Java layer. A tag that completes on
 * another thread keeps the contextual instance, so it reads both layers without an active request context. This class
 * is internal. Application code does not use it.
 */
@RequestScoped
public class SeoLayers {

    private final SeoValues java = new SeoValues();
    private volatile SeoValues tag = new SeoValues();

    /**
     * Return the values from the tag parameters.
     *
     * @return the tag layer
     */
    public SeoValues tag() {
        return tag;
    }

    /**
     * Store the values from the tag parameters.
     *
     * @param values the tag layer, where null stores an empty layer
     */
    public void tag(SeoValues values) {
        this.tag = values == null ? new SeoValues() : values;
    }

    /**
     * Return the values set in Java.
     *
     * @return the Java layer
     */
    public SeoValues java() {
        return java;
    }
}
