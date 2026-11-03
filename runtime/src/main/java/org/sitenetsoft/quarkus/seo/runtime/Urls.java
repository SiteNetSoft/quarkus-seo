package org.sitenetsoft.quarkus.seo.runtime;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Build and check URLs.
 */
final class Urls {

    private static final Pattern SAFE_HOST = Pattern.compile("(\\[[0-9A-Fa-f:.]+\\]|[A-Za-z0-9._-]+)(:[0-9]{1,5})?");

    private Urls() {
    }

    /**
     * Turn a path or URL into an absolute URL.
     *
     * @param base the base URL, with or without a trailing slash
     * @param pathOrUrl an absolute URL, a scheme-relative URL or a path
     * @return the absolute URL
     */
    static String absolute(String base, String pathOrUrl) {
        String cleanBase = stripTrailingSlashes(base);
        if (pathOrUrl == null || pathOrUrl.isEmpty()) {
            return cleanBase;
        }
        String lower = pathOrUrl.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return pathOrUrl;
        }
        if (pathOrUrl.startsWith("//")) {
            int colon = cleanBase.indexOf(':');
            String scheme = colon > 0 ? cleanBase.substring(0, colon) : "https";
            return scheme + ":" + pathOrUrl;
        }
        int start = 0;
        while (start < pathOrUrl.length() && pathOrUrl.charAt(start) == '/') {
            start++;
        }
        return cleanBase + "/" + pathOrUrl.substring(start);
    }

    /**
     * Keep only the listed query parameters.
     *
     * @param rawQuery the raw query string without the question mark, or null
     * @param keep the parameter names to keep, compared with the decoded names
     * @return the kept parameters in request order, or an empty string
     */
    static String canonicalQuery(String rawQuery, List<String> keep) {
        if (rawQuery == null || rawQuery.isEmpty() || keep.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String rawName = equals < 0 ? pair : pair.substring(0, equals);
            if (keep.contains(decodedName(rawName))) {
                if (result.length() > 0) {
                    result.append('&');
                }
                result.append(pair);
            }
        }
        return result.toString();
    }

    /**
     * Decode a query parameter name.
     *
     * @param rawName the name as it appears in the query
     * @return the decoded name, or the raw name when its percent-encoding is malformed
     */
    private static String decodedName(String rawName) {
        try {
            return URLDecoder.decode(rawName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return rawName;
        }
    }

    /**
     * Check that a host value holds only a host name or address and an optional port.
     *
     * @param host the host value, for example from a Host header
     * @return true when the value is safe to write into a URL
     */
    static boolean isSafeHost(String host) {
        return host != null && SAFE_HOST.matcher(host).matches();
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
