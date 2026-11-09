package org.sitenetsoft.quarkus.seo.runtime;

/**
 * Normalize the output paths so that routes, ownership checks and generated links agree.
 *
 * <p>
 * This class is internal. Application code does not use it.
 */
public final class SeoPaths {

    /** The path of the robots file, relative to the host root or the root path. */
    public static final String ROBOTS_TXT = "/robots.txt";

    /** The path of the security contact file, relative to the host root or the root path. */
    public static final String SECURITY_TXT = "/.well-known/security.txt";

    /** The path of the llms.txt file, relative to the host root or the root path. */
    public static final String LLMS_TXT = "/llms.txt";

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
