package org.sitenetsoft.quarkus.seo.runtime.devui;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Fetch pages of the running application over HTTP so that the dev capture records their heads.
 *
 * <p>
 * The fetcher only ever sends requests to {@code localhost}. It accepts a path, never a URL, so a value from the Dev
 * UI cannot make the application fetch another host.
 */
final class PageFetcher {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_PORT = 8080;

    private final HttpClient client;
    private final String rootPath;

    /**
     * Create a fetcher with its own HTTP client.
     *
     * @param rootPath the normalized HTTP root path, or an empty string for {@code /}
     */
    PageFetcher(String rootPath) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.rootPath = rootPath;
    }

    /**
     * Accept a value as a local path.
     *
     * <p>
     * A local path starts with exactly one slash. It holds no backslash, no control character and no whitespace.
     *
     * @param input the value from the Dev UI
     * @return the path, or empty when the value is not a local path
     */
    static Optional<String> localPath(String input) {
        if (input == null || input.isEmpty() || input.charAt(0) != '/') {
            return Optional.empty();
        }
        if (input.length() > 1 && input.charAt(1) == '/') {
            return Optional.empty();
        }
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' || Character.isISOControl(c) || Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                return Optional.empty();
            }
        }
        return Optional.of(input);
    }

    /**
     * Return the key under which the dev capture records a path.
     *
     * @param path a local path, which may hold a query or a fragment
     * @return the path without its query and fragment
     */
    static String capturePath(String path) {
        int end = path.length();
        int query = path.indexOf('?');
        if (query >= 0) {
            end = query;
        }
        int fragment = path.indexOf('#');
        if (fragment >= 0 && fragment < end) {
            end = fragment;
        }
        return path.substring(0, end);
    }

    /**
     * Send a GET request for a local path and discard the body.
     *
     * @param path the path relative to the HTTP root path
     * @return the HTTP status
     * @throws IllegalArgumentException when the path is not a local path
     * @throws IOException when the request fails
     * @throws InterruptedException when the thread is interrupted during the request
     */
    int fetch(String path) throws IOException, InterruptedException {
        String local = localPath(path).orElseThrow(() -> new IllegalArgumentException("Not a local path: " + path));
        URI uri = URI.create("http://localhost:" + port() + rootPath + local);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static int port() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.http.port", Integer.class).orElse(DEFAULT_PORT);
    }
}
