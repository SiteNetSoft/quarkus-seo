package org.sitenetsoft.quarkus.seo.runtime.devui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.runtime.SeoChecks;
import org.sitenetsoft.quarkus.seo.runtime.SeoWarning;

/**
 * Record the last resolved head of each path and log its content warnings once per path.
 *
 * <p>
 * The key of a path is {@code RequestInfo.path()}. That value is relative to the HTTP root path and starts with a
 * slash. A caller that fetches a page adds the root path in front of the key and reads the result with the key.
 *
 * <p>
 * The bean exists in dev mode only. The capture keeps the heads of at most 1,000 paths and drops the oldest head
 * first. It remembers at most 10,000 logged paths and drops the oldest path first. The logged set has the larger bound
 * because a logged path costs one key, and a sitemap check marks every URL of the sitemap.
 */
@Singleton
public class SeoDevCapture {

    private static final Logger LOG = Logger.getLogger("org.sitenetsoft.quarkus.seo.runtime.SeoDevCapture");
    private static final int MAX_PATHS = 1000;
    private static final int MAX_LOGGED = 10_000;

    private final Map<String, ResolvedSeo> captured = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ResolvedSeo> eldest) {
            return size() > MAX_PATHS;
        }
    };

    private final Map<String, Boolean> logged = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_LOGGED;
        }
    };

    /**
     * Store the result of a path and log its warnings the first time the path appears.
     *
     * @param path the request path, relative to the HTTP root path
     * @param seo the resolved head
     */
    public void record(String path, ResolvedSeo seo) {
        boolean first;
        synchronized (captured) {
            captured.put(path, seo);
            first = logged.put(path, Boolean.TRUE) == null;
        }
        if (first) {
            List<SeoWarning> warnings = SeoChecks.page(seo);
            for (SeoWarning warning : warnings) {
                LOG.warn("SEO check on " + path + ": " + warning.message() + " [" + warning.code() + "]");
            }
        }
    }

    /**
     * Return the last result of a path.
     *
     * @param path the request path, relative to the HTTP root path
     * @return the last resolved head, or empty when the path is unknown
     */
    public Optional<ResolvedSeo> last(String path) {
        synchronized (captured) {
            return Optional.ofNullable(captured.get(path));
        }
    }

    /**
     * Forget the stored result of a path and mark the path as logged.
     *
     * <p>
     * The Dev UI calls this method before it fetches a page. The next result of the path is stored and read. The path
     * stays marked as logged for the rest of the dev session, so neither the Dev UI fetch nor later browsing logs its
     * warnings. The Dev UI shows them instead.
     *
     * @param path the request path, relative to the HTTP root path
     */
    public void expect(String path) {
        synchronized (captured) {
            captured.remove(path);
            logged.put(path, Boolean.TRUE);
        }
    }

    /**
     * Tell whether the warnings of a path are already logged or suppressed.
     *
     * @param path the request path, relative to the HTTP root path
     * @return true when the path is in the logged set
     */
    boolean logged(String path) {
        synchronized (captured) {
            return logged.containsKey(path);
        }
    }
}
