package org.sitenetsoft.quarkus.seo.runtime;

import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

import org.sitenetsoft.quarkus.seo.schema.JsonWriter;

/**
 * Write the JSON text of a web manifest.
 */
final class ManifestWriter {

    private ManifestWriter() {
    }

    /**
     * Write the manifest in a fixed key order and leave out the keys that have no value.
     * The method also leaves out an icon that has no URL.
     *
     * @param config the manifest configuration
     * @param siteName the site name, used when the configuration sets no name
     * @param themeColor the site theme color, used when the configuration sets no color
     * @param rootPath the HTTP root path, which is empty for {@code /}
     * @return the JSON text
     */
    static String write(SeoConfig.Manifest config, Optional<String> siteName, Optional<String> themeColor,
            String rootPath) {
        StringBuilder out = new StringBuilder("{");
        boolean[] first = { true };
        member(out, first, "name", config.name().or(() -> siteName).orElse(null));
        member(out, first, "short_name", config.shortName().orElse(null));
        member(out, first, "start_url", config.startUrl().filter(url -> !url.isBlank()).map(url -> url(rootPath, url))
                .orElse(rootPath + "/"));
        member(out, first, "display", config.display().map(ManifestWriter::display).orElse(null));
        member(out, first, "theme_color", config.themeColor().or(() -> themeColor).orElse(null));
        member(out, first, "background_color", config.backgroundColor().orElse(null));
        StringBuilder icons = new StringBuilder();
        for (SeoConfig.Icon icon : new TreeMap<>(config.icons()).values()) {
            if (icon.src().isPresent() && !icon.src().get().isBlank()) {
                if (icons.length() > 0) {
                    icons.append(',');
                }
                icon(icons, icon, rootPath);
            }
        }
        if (icons.length() > 0) {
            separator(out, first);
            out.append("\"icons\":[").append(icons).append(']');
        }
        return out.append('}').toString();
    }

    private static void icon(StringBuilder out, SeoConfig.Icon icon, String rootPath) {
        out.append('{');
        boolean[] first = { true };
        member(out, first, "src", url(rootPath, icon.src().get()));
        member(out, first, "sizes", icon.sizes().orElse(null));
        member(out, first, "type", icon.type().orElse(null));
        out.append('}');
    }

    private static String display(SeoConfig.Display display) {
        return display.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String url(String rootPath, String value) {
        String url = value.trim();
        if (url.startsWith("/") && !url.startsWith("//")) {
            return rootPath + url;
        }
        return url;
    }

    private static void member(StringBuilder out, boolean[] first, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        separator(out, first);
        JsonWriter.string(out, key);
        out.append(':');
        JsonWriter.string(out, value);
    }

    private static void separator(StringBuilder out, boolean[] first) {
        if (!first[0]) {
            out.append(',');
        }
        first[0] = false;
    }
}
