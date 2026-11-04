package org.sitenetsoft.quarkus.seo.runtime;

/**
 * Hold the IndexNow key, as the build found it.
 *
 * @param key the key
 * @param underRootPath true when the key file is served under the HTTP root path
 */
public record IndexNowKey(String key, boolean underRootPath) {
}
