package org.sitenetsoft.quarkus.seo.runtime;

import java.util.Optional;

/**
 * Hold the href of the manifest link, as the build found it.
 *
 * @param href the href, or empty when the extension serves no manifest
 */
public record ManifestLink(Optional<String> href) {
}
