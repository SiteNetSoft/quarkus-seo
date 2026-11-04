package org.sitenetsoft.quarkus.seo.runtime;

/**
 * Hold one content problem found by a check.
 *
 * @param code the stable code of the problem
 * @param message one plain sentence that describes the problem
 */
public record SeoWarning(String code, String message) {
}
