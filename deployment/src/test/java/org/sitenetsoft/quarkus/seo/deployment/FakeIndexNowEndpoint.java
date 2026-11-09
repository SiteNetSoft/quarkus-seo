package org.sitenetsoft.quarkus.seo.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

/**
 * Fake IndexNow endpoint for the tests: a server on a free port, the bodies and content types it got, and the status
 * it answers.
 *
 * <p>
 * The pom pins the Quarkus test port to a random one, so the endpoint cannot be a route of the test application.
 * QuarkusUnitTest loads a test class twice, in two class loaders. The config comes from the first load and the test
 * body runs in the second, so both loads must see one server. The shared state lives in the system properties, held
 * as JDK types only. A test that uses this class adds it to its archive with {@code jar.addClass}.
 */
final class FakeIndexNowEndpoint {

    final List<String> bodies;
    final List<String> contentTypes;
    final AtomicInteger status;
    private final HttpServer server;

    private FakeIndexNowEndpoint(HttpServer server, List<String> bodies, List<String> contentTypes,
            AtomicInteger status) {
        this.server = server;
        this.bodies = bodies;
        this.contentTypes = contentTypes;
        this.status = status;
    }

    /**
     * Start the fake for a test class, or return the one that another load of the class started.
     *
     * @param key the name of the test class
     * @return the fake
     */
    @SuppressWarnings("unchecked")
    static FakeIndexNowEndpoint of(String key) {
        synchronized (System.getProperties()) {
            Object[] parts = (Object[]) System.getProperties().get(key);
            if (parts == null) {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                    List<String> bodies = new CopyOnWriteArrayList<>();
                    List<String> types = new CopyOnWriteArrayList<>();
                    AtomicInteger status = new AtomicInteger(202);
                    server.createContext("/fake-indexnow", exchange -> {
                        types.add(exchange.getRequestHeaders().getFirst("Content-Type"));
                        bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                        exchange.sendResponseHeaders(status.get(), -1);
                        exchange.close();
                    });
                    server.start();
                    parts = new Object[] { server, bodies, types, status };
                    System.getProperties().put(key, parts);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return new FakeIndexNowEndpoint((HttpServer) parts[0], (List<String>) parts[1], (List<String>) parts[2],
                    (AtomicInteger) parts[3]);
        }
    }

    /**
     * Return the URL that the fake listens on.
     *
     * @return the endpoint URL
     */
    String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/fake-indexnow";
    }

    /**
     * Stop the server.
     */
    void stop() {
        server.stop(0);
    }
}
