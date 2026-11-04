package org.sitenetsoft.quarkus.seo.runtime;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Write the text of a security.txt file (RFC 9116).
 */
final class SecurityTxtWriter {

    private SecurityTxtWriter() {
    }

    /**
     * Write the file in the field order of the RFC.
     *
     * @param config the security.txt configuration
     * @param canonicalUrl the URL of the file
     * @return the file text, or empty when no contact is set
     */
    static Optional<String> write(SeoConfig.SecurityTxt config, String canonicalUrl) {
        StringBuilder text = new StringBuilder();
        for (String contact : config.contact().orElse(List.of())) {
            String value = clean(contact);
            if (!value.isEmpty()) {
                text.append("Contact: ").append(withScheme(value)).append('\n');
            }
        }
        if (text.length() == 0) {
            return Optional.empty();
        }
        Optional<OffsetDateTime> expires = config.expires().flatMap(SecurityTxtWriter::expires);
        if (expires.isPresent()) {
            text.append("Expires: ").append(DateTimeFormatter.ISO_INSTANT.format(expires.get())).append('\n');
        }
        line(text, "Encryption", config.encryption().orElse(""));
        line(text, "Policy", config.policy().orElse(""));
        String languages = "";
        for (String language : config.preferredLanguages().orElse(List.of())) {
            String value = clean(language);
            if (!value.isEmpty()) {
                languages = languages.isEmpty() ? value : languages + ", " + value;
            }
        }
        line(text, "Preferred-Languages", languages);
        line(text, "Hiring", config.hiring().orElse(""));
        line(text, "Canonical", canonicalUrl);
        return Optional.of(text.toString());
    }

    /**
     * Parse an expiry value.
     *
     * @param value an ISO 8601 date or date-time, or null
     * @return the instant in UTC, or empty when the value is blank or does not parse
     */
    static Optional<OffsetDateTime> expires(String value) {
        String clean = value == null ? "" : clean(value);
        if (clean.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(clean).withOffsetSameInstant(ZoneOffset.UTC));
        } catch (DateTimeException e) {
            // Try a plain date next.
        }
        try {
            return Optional.of(LocalDate.parse(clean).atStartOfDay().atOffset(ZoneOffset.UTC));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    private static void line(StringBuilder text, String field, String value) {
        String clean = clean(value);
        if (!clean.isEmpty()) {
            text.append(field).append(": ").append(clean).append('\n');
        }
    }

    /**
     * Trim a value and cut it at the first line break.
     *
     * @param value the raw value
     * @return the first line of the value, trimmed
     */
    private static String clean(String value) {
        int end = value.length();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r') {
                end = i;
                break;
            }
        }
        return value.substring(0, end).trim();
    }

    private static String withScheme(String contact) {
        if (contact.indexOf(':') < 0 && contact.indexOf('@') > 0) {
            return "mailto:" + contact;
        }
        return contact;
    }
}
