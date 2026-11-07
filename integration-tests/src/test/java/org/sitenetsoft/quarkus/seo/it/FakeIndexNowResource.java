package org.sitenetsoft.quarkus.seo.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Run a fake IndexNow endpoint on a free port in the test JVM. A POST records its body. A GET returns the recorded
 * bodies, so the test also works against a packaged or native application.
 */
public class FakeIndexNowResource implements QuarkusTestResourceLifecycleManager {

    /** The system property that holds the URL of the fake. */
    static final String URL_PROPERTY = "fake.indexnow.url";

    private HttpServer server;

    @Override
    public Map<String, String> start() {
        List<String> bodies = new CopyOnWriteArrayList<>();
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/indexnow", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.sendResponseHeaders(202, -1);
            } else {
                byte[] out = String.join("\n", bodies).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
            }
            exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/indexnow";
        System.setProperty(URL_PROPERTY, url);
        return Map.of("quarkus.seo.indexnow.endpoint", url);
    }

    @Override
    public void stop() {
        System.clearProperty(URL_PROPERTY);
        if (server != null) {
            server.stop(0);
        }
    }
}
