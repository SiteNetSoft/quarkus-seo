package org.sitenetsoft.quarkus.seo.runtime.devui;

/**
 * Hold whether the extension serves security.txt, as the build found it.
 *
 * @param served true when the extension serves the file, false when it is off or the application serves it
 */
public record SecurityTxtServed(boolean served) {
}
