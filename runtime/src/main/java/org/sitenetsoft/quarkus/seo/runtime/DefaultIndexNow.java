package org.sitenetsoft.quarkus.seo.runtime;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.DefaultBean;
import org.sitenetsoft.quarkus.seo.IndexNow;

/**
 * Submit URLs to the IndexNow endpoint with the JDK HTTP client.
 */
@DefaultBean
@Singleton
public class DefaultIndexNow implements IndexNow {

    private static final Logger LOG = Logger.getLogger(DefaultIndexNow.class);
    private static final int MAX_URLS = 10_000;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;
    private final String endpoint;
    private final IndexNowKey key;
    private final SeoSettings settings;
    private final Optional<String> siteUrl;

    /**
     * Create the bean from the configuration.
     *
     * @param config the SEO configuration
     * @param key the IndexNow key
     * @param settings the resolved site settings
     */
    @Inject
    public DefaultIndexNow(SeoConfig config, IndexNowKey key, SeoSettings settings) {
        this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT).build(), config.indexNow().endpoint(), key,
                settings, config.siteUrl());
    }

    /**
     * Create the bean from its parts.
     *
     * @param client the HTTP client
     * @param endpoint the IndexNow endpoint
     * @param key the IndexNow key
     * @param settings the resolved site settings
     * @param siteUrl the site URL, empty when unset
     */
    DefaultIndexNow(HttpClient client, String endpoint, IndexNowKey key, SeoSettings settings,
            Optional<String> siteUrl) {
        this.client = client;
        this.endpoint = endpoint;
        this.key = key;
        this.settings = settings;
        this.siteUrl = siteUrl;
    }

    @Override
    public CompletionStage<Void> submit(String... paths) {
        return submit(paths == null ? List.<String> of() : Arrays.asList(paths));
    }

    @Override
    public CompletionStage<Void> submit(Collection<String> paths) {
        try {
            return send(paths);
        } catch (RuntimeException e) {
            LOG.debugf(e, "IndexNow submission failed");
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletionStage<Void> send(Collection<String> paths) {
        if (!settings.indexable()) {
            LOG.debug("IndexNow skipped: the site is not indexable");
            return CompletableFuture.completedFuture(null);
        }
        if (siteUrl.isEmpty() || siteUrl.get().isBlank()) {
            LOG.debug("IndexNow skipped: quarkus.seo.site-url is not set");
            return CompletableFuture.completedFuture(null);
        }
        String base = siteUrl.get();
        String host = hostOf(base);
        if (host == null || paths == null) {
            return CompletableFuture.completedFuture(null);
        }
        Set<String> urls = new LinkedHashSet<>();
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            String url = Urls.absolute(base, path.trim());
            if (host.equalsIgnoreCase(hostOf(url))) {
                urls.add(url);
            }
        }
        if (urls.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        String file = "/" + key.key() + ".txt";
        String keyLocation = key.underRootPath() ? Urls.absolute(base, file) : Urls.hostRoot(base) + file;
        List<CompletableFuture<Void>> requests = new ArrayList<>();
        for (String body : body(host, key.key(), keyLocation, new ArrayList<>(urls))) {
            requests.add(post(body));
        }
        return CompletableFuture.allOf(requests.toArray(new CompletableFuture<?>[0]));
    }

    private CompletableFuture<Void> post(String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .handle((response, failure) -> {
                        if (failure != null) {
                            LOG.debugf(failure, "IndexNow request to %s failed", endpoint);
                        } else if (response.statusCode() != 200 && response.statusCode() != 202) {
                            LOG.debugf("IndexNow endpoint %s answered %d", endpoint, response.statusCode());
                        }
                        return (Void) null;
                    });
        } catch (RuntimeException e) {
            LOG.debugf(e, "IndexNow request to %s failed", endpoint);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Build the JSON bodies of the IndexNow requests.
     *
     * @param host the host of the site
     * @param key the IndexNow key
     * @param keyLocation the absolute URL of the key file
     * @param urls the absolute URLs
     * @return one body per 10,000 URLs
     */
    static List<String> body(String host, String key, String keyLocation, List<String> urls) {
        List<String> bodies = new ArrayList<>();
        for (int from = 0; from < urls.size(); from += MAX_URLS) {
            StringBuilder out = new StringBuilder("{\"host\":");
            Json.string(out, host);
            out.append(",\"key\":");
            Json.string(out, key);
            out.append(",\"keyLocation\":");
            Json.string(out, keyLocation);
            out.append(",\"urlList\":[");
            int to = Math.min(urls.size(), from + MAX_URLS);
            for (int i = from; i < to; i++) {
                if (i > from) {
                    out.append(',');
                }
                Json.string(out, urls.get(i));
            }
            bodies.add(out.append("]}").toString());
        }
        return bodies;
    }
}
