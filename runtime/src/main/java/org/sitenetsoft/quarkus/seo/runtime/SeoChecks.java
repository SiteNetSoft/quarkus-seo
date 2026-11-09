package org.sitenetsoft.quarkus.seo.runtime;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sitenetsoft.quarkus.seo.ResolvedSeo;

/**
 * Check the resolved values and the configuration for content problems.
 */
public final class SeoChecks {

    private static final int MAX_TITLE = 60;
    private static final int MIN_DESCRIPTION = 50;
    private static final int MAX_DESCRIPTION = 160;
    private static final Duration EXPIRY_WINDOW = Duration.ofDays(30);

    private SeoChecks() {
    }

    /**
     * Check the title, description and image of one page.
     *
     * @param seo the resolved values of the page
     * @return the warnings in a fixed order, empty when the page is clean
     */
    public static List<SeoWarning> page(ResolvedSeo seo) {
        List<SeoWarning> warnings = new ArrayList<>();
        String title = seo.title() == null ? "" : seo.title();
        int titleLength = title.codePointCount(0, title.length());
        if (title.isBlank()) {
            warnings.add(new SeoWarning("missing-title", "The page has no title."));
        } else if (titleLength > MAX_TITLE) {
            warnings.add(new SeoWarning("long-title",
                    "The title has " + titleLength + " characters. Keep it at " + MAX_TITLE + " or fewer."));
        }
        Optional<String> description = seo.description().filter(value -> !value.isBlank());
        if (description.isEmpty()) {
            warnings.add(new SeoWarning("missing-description", "The page has no description."));
        } else {
            String text = description.get();
            int length = text.codePointCount(0, text.length());
            if (length < MIN_DESCRIPTION || length > MAX_DESCRIPTION) {
                warnings.add(new SeoWarning("description-length", "The description has " + length
                        + " characters. Keep it between " + MIN_DESCRIPTION + " and " + MAX_DESCRIPTION + "."));
            }
        }
        if (seo.image().isEmpty()) {
            warnings.add(new SeoWarning("missing-image", "The page has no image."));
        } else if (!isAbsolute(seo.image().get().url())) {
            warnings.add(new SeoWarning("relative-image",
                    "The image URL " + seo.image().get().url() + " is not absolute."));
        }
        return warnings;
    }

    /**
     * Check that every alternate page links back to the page.
     *
     * @param pageUrl the absolute URL of the page
     * @param links the alternate links of the page
     * @param alternatesByUrl the alternate links of the pages that were fetched, by URL
     * @return one warning for each alternate page without a return link
     */
    public static List<SeoWarning> oneWayHreflang(String pageUrl, List<ResolvedSeo.Link> links,
            Map<String, List<ResolvedSeo.Link>> alternatesByUrl) {
        List<SeoWarning> warnings = new ArrayList<>();
        for (ResolvedSeo.Link link : links) {
            if ("x-default".equals(link.hreflang()) || link.href().equals(pageUrl)) {
                continue;
            }
            List<ResolvedSeo.Link> back = alternatesByUrl.get(link.href());
            if (back == null) {
                continue;
            }
            boolean returns = false;
            for (ResolvedSeo.Link other : back) {
                if (other.href().equals(pageUrl)) {
                    returns = true;
                    break;
                }
            }
            if (!returns) {
                warnings.add(new SeoWarning("one-way-hreflang",
                        "The page " + link.href() + " does not link back to " + pageUrl + "."));
            }
        }
        return warnings;
    }

    /**
     * Find titles that more than one page uses.
     *
     * @param titleByUrl the title of each page, by URL
     * @return one warning for each repeated title, which names every URL
     */
    public static List<SeoWarning> duplicateTitles(Map<String, String> titleByUrl) {
        Map<String, List<String>> urlsByTitle = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : titleByUrl.entrySet()) {
            String title = entry.getValue();
            if (title != null && !title.isBlank()) {
                urlsByTitle.computeIfAbsent(title, key -> new ArrayList<>()).add(entry.getKey());
            }
        }
        List<SeoWarning> warnings = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : urlsByTitle.entrySet()) {
            if (entry.getValue().size() > 1) {
                warnings.add(new SeoWarning("duplicate-title", "The title \"" + entry.getKey()
                        + "\" appears on " + String.join(", ", entry.getValue()) + "."));
            }
        }
        return warnings;
    }

    /**
     * Check the expiry of the security.txt file when the extension serves a file with at least one contact.
     *
     * @param config the security.txt configuration
     * @param served whether the extension serves the security.txt file
     * @param now the current time
     * @return the warnings about the expiry, empty when no file is served or the expiry is far enough away
     */
    public static List<SeoWarning> servedSecurityTxt(SeoConfig.SecurityTxt config, boolean served, Instant now) {
        if (!served || SecurityTxtWriter.write(config, "").isEmpty()) {
            return List.of();
        }
        return securityTxt(config, now);
    }

    /**
     * Check the expiry of the security.txt file.
     *
     * @param config the security.txt configuration
     * @param now the current time
     * @return the warnings about the expiry, empty when it is far enough away
     */
    public static List<SeoWarning> securityTxt(SeoConfig.SecurityTxt config, Instant now) {
        Optional<String> value = config.expires().filter(text -> !text.isBlank());
        if (value.isEmpty()) {
            return List.of(new SeoWarning("security-txt-expires-missing",
                    "The security.txt file has no expiry date. Set quarkus.seo.security-txt.expires."));
        }
        Optional<OffsetDateTime> expires = SecurityTxtWriter.expires(value.get());
        if (expires.isEmpty()) {
            return List.of(new SeoWarning("security-txt-expires-invalid",
                    "The security.txt expiry " + value.get() + " is not an ISO 8601 date."));
        }
        Instant at = expires.get().toInstant();
        if (!at.isAfter(now)) {
            return List.of(new SeoWarning("security-txt-expired", "The security.txt file expired on " + at + "."));
        }
        if (at.isBefore(now.plus(EXPIRY_WINDOW))) {
            return List.of(new SeoWarning("security-txt-expires-soon",
                    "The security.txt file expires on " + at + ", which is less than 30 days away."));
        }
        return List.of();
    }

    private static boolean isAbsolute(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://"));
    }
}
