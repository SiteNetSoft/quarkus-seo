package org.sitenetsoft.quarkus.seo;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

/**
 * Tell IndexNow search engines that URLs changed.
 *
 * <p>
 * The bean exists when {@code quarkus.seo.enabled} and {@code quarkus.seo.indexnow.enabled} are both true. It exists
 * also when the application serves the key file itself, because the application file only replaces the key route.
 * The extension does nothing when the site is not indexable or when {@code quarkus.seo.site-url} is unset, because a
 * background job has no request to take a host from.
 */
public interface IndexNow {

    /**
     * Submit paths or absolute URLs.
     *
     * <p>
     * A path resolves against the site URL. A URL on another host is dropped. The call does not block, and the stage
     * always completes normally, also when the endpoint fails or cannot be reached.
     *
     * @param paths the paths or absolute URLs
     * @return the stage that completes when the requests end
     */
    CompletionStage<Void> submit(String... paths);

    /**
     * Submit paths or absolute URLs.
     *
     * @param paths the paths or absolute URLs
     * @return the stage that completes when the requests end
     * @see #submit(String...)
     */
    CompletionStage<Void> submit(Collection<String> paths);
}
