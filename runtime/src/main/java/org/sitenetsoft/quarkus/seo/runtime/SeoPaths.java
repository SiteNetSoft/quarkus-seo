package org.sitenetsoft.quarkus.seo.runtime;

/**
 * Normalize the output paths so that routes, ownership checks and generated links agree.
 *
 * <p>
 * This class is internal. Application code does not use it.
 */
public final class SeoPaths {

    private SeoPaths() {
    }

    /**
     * Normalize a path.
     *
     * <p>
     * The method trims the value, adds a leading slash, collapses repeated slashes and strips the trailing slash.
     * The root path keeps its single slash.
     *
     * @param path the path, where null or blank stands for the root path
     * @return the normalized path, for example {@code /maps/sitemap.xml} or {@code /}
     */
    public static String normalize(String path) {
        if (path == null) {
            return "/";
        }
        String trimmed = path.trim();
        StringBuilder result = new StringBuilder(trimmed.length() + 1);
        result.append('/');
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c != '/' || result.charAt(result.length() - 1) != '/') {
                result.append(c);
            }
        }
        if (result.length() > 1 && result.charAt(result.length() - 1) == '/') {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }
}
