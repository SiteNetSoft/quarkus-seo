package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.seo.IndexNow;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.QuarkusUnitTest;

class IndexNowSubmitTest {

    // The pom pins the Quarkus test port to a random one, so the fake endpoint is its own server. QuarkusUnitTest
    // loads this class twice, in two class loaders, and the config comes from the first load while the test body
    // runs in the second. The fake therefore lives in the system properties, held as JDK types only.
    static final Fake FAKE = Fake.of(IndexNowSubmitTest.class.getName());

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.seo.indexnow.enabled", "true")
            .overrideConfigKey("quarkus.seo.indexnow.key", "abcd1234")
            .overrideConfigKey("quarkus.seo.indexnow.endpoint",
                    FAKE.endpoint())
            .overrideConfigKey("quarkus.seo.site-url", "https://ex.com")
            .overrideConfigKey("quarkus.seo.indexable", "true");

    @Inject
    IndexNow indexNow;

    @BeforeEach
    void clear() {
        FAKE.bodies.clear();
        FAKE.contentTypes.clear();
    }

    @Test
    void postsTheUrlsAsJson() throws Exception {
        indexNow.submit("/a", "/b").toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(FAKE.bodies).hasSize(1);
        assertThat(FAKE.bodies.get(0)).contains("\"urlList\":[\"https://ex.com/a\",\"https://ex.com/b\"]")
                .contains("\"key\":\"abcd1234\"");
        assertThat(FAKE.contentTypes).isEqualTo(List.of("application/json; charset=utf-8"));
    }

    @AfterAll
    static void stopFake() {
        FAKE.server.stop(0);
    }

    /** The fake endpoint: a server, the bodies and content types it got, and the status it answers. */
    record Fake(HttpServer server, List<String> bodies, List<String> contentTypes, AtomicInteger status) {

        @SuppressWarnings("unchecked")
        static Fake of(String key) {
            synchronized (System.getProperties()) {
                Object[] parts = (Object[]) System.getProperties().get(key);
                if (parts == null) {
                    try {
                        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                        parts = new Object[] { server, new CopyOnWriteArrayList<String>(),
                                new CopyOnWriteArrayList<String>(), new AtomicInteger(202) };
                        List<String> bodies = (List<String>) parts[1];
                        List<String> types = (List<String>) parts[2];
                        AtomicInteger status = (AtomicInteger) parts[3];
                        server.createContext("/fake-indexnow", exchange -> {
                            types.add(exchange.getRequestHeaders().getFirst("Content-Type"));
                            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                            exchange.sendResponseHeaders(status.get(), -1);
                            exchange.close();
                        });
                        server.start();
                        System.getProperties().put(key, parts);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return new Fake((HttpServer) parts[0], (List<String>) parts[1], (List<String>) parts[2],
                        (AtomicInteger) parts[3]);
            }
        }

        String endpoint() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/fake-indexnow";
        }
    }
}
