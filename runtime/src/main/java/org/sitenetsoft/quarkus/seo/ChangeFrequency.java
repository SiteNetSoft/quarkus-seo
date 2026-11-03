package org.sitenetsoft.quarkus.seo;

/**
 * Tell crawlers how often a page changes.
 */
public enum ChangeFrequency {
    /** The page changes on every access. */
    ALWAYS,
    /** The page changes every hour. */
    HOURLY,
    /** The page changes every day. */
    DAILY,
    /** The page changes every week. */
    WEEKLY,
    /** The page changes every month. */
    MONTHLY,
    /** The page changes every year. */
    YEARLY,
    /** The page never changes. */
    NEVER
}
