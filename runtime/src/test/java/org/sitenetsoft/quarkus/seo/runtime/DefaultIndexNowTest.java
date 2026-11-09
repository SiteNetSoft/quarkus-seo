package org.sitenetsoft.quarkus.seo.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitenetsoft.quarkus.seo.runtime.DefaultIndexNow.body;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class DefaultIndexNowTest {

    private HttpServer server;
    private final List<String> received = new CopyOnWriteArrayList<>();
    private String endpoint;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/indexnow", exchange -> {
            received.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/indexnow";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private DefaultIndexNow indexNow(Optional<String> siteUrl, boolean underRootPath) {
        SeoSettings settings = new SeoSettings(Optional.empty(), true, null, Optional.empty(), "/");
        return new DefaultIndexNow(HttpClient.newHttpClient(), endpoint, new IndexNowKey("abcd1234", underRootPath),
                settings, siteUrl);
    }

    @Test
    void bodyHoldsHostKeyKeyLocationAndUrls() {
        assertThat(body("ex.com", "abcd1234", "https://ex.com/abcd1234.txt", List.of("https://ex.com/a")))
                .containsExactly("{\"host\":\"ex.com\",\"key\":\"abcd1234\",\"keyLocation\":\"https://ex.com/abcd1234.txt\","
                        + "\"urlList\":[\"https://ex.com/a\"]}");
    }

    @Test
    void splitsAt10000Urls() {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < 10_001; i++) {
            urls.add("https://ex.com/" + i);
        }
        List<String> bodies = body("ex.com", "abcd1234", "https://ex.com/abcd1234.txt", urls);
        assertThat(bodies).hasSize(2);
        assertThat(bodies.get(0).split("\"https://ex.com/[0-9]+\"", -1)).hasSize(10_001);
        assertThat(bodies.get(0)).contains("\"https://ex.com/9999\"").doesNotContain("\"https://ex.com/10000\"");
        assertThat(bodies.get(1)).contains("\"urlList\":[\"https://ex.com/10000\"]");
    }

    @Test
    void relativePathsUseTheSiteUrlAndOtherHostsAreDropped() throws Exception {
        indexNow(Optional.of("https://ex.com"), false)
                .submit("/a", "https://other.com/b", "https://ex.com/c").toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(received).hasSize(1);
        assertThat(received.get(0)).contains("\"urlList\":[\"https://ex.com/a\",\"https://ex.com/c\"]")
                .contains("\"host\":\"ex.com\"").contains("\"keyLocation\":\"https://ex.com/abcd1234.txt\"");
    }

    @Test
    void keyLocationUsesTheHostRootUnlessServedUnderTheRootPath() throws Exception {
        indexNow(Optional.of("https://ex.com/shop"), false).submit("/a").toCompletableFuture().get(10,
                TimeUnit.SECONDS);
        indexNow(Optional.of("https://ex.com/shop"), true).submit("/a").toCompletableFuture().get(10,
                TimeUnit.SECONDS);
        assertThat(received.get(0)).contains("\"keyLocation\":\"https://ex.com/abcd1234.txt\"")
                .contains("\"urlList\":[\"https://ex.com/shop/a\"]");
        assertThat(received.get(1)).contains("\"keyLocation\":\"https://ex.com/shop/abcd1234.txt\"");
    }

    @Test
    void duplicatesKeepTheFirstPosition() throws Exception {
        indexNow(Optional.of("https://ex.com"), false).submit("/a", "/b", "/a", "https://ex.com/b")
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(received.get(0)).contains("\"urlList\":[\"https://ex.com/a\",\"https://ex.com/b\"]");
    }

    @Test
    void noSiteUrlDoesNothing() throws Exception {
        indexNow(Optional.empty(), false).submit("/a").toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(received).isEmpty();
    }
}
