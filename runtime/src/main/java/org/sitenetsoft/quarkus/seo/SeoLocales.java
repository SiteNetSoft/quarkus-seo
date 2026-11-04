package org.sitenetsoft.quarkus.seo;

import java.util.List;
import java.util.Locale;

/**
 * Supply the locales of the site to the sitemap, the hreflang links and og:locale.
 *
 * <p>
 * An application whose languages change at run time provides its own bean of this type.
 */
public interface SeoLocales {

    /**
     * List the locales the site serves.
     *
     * @return the locales, never empty
     */
    List<Locale> locales();

    /**
     * Name the locale of pages that have no locale in their path.
     *
     * @return the default locale
     */
    Locale defaultLocale();
}
