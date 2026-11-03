package org.sitenetsoft.quarkus.seo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put the annotated page method into the sitemap.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SitemapEntry {

    /**
     * Set the change frequency, as the lower case name of a {@link ChangeFrequency}.
     *
     * @return the change frequency, or an empty string to leave it out
     */
    String changefreq() default "";

    /**
     * Set the priority between 0.0 and 1.0.
     *
     * @return the priority, or a negative value to leave it out
     */
    double priority() default -1;
}
