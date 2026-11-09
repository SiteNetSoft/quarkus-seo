package org.sitenetsoft.quarkus.seo.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Optional;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

class RestPathsTest {

    @ApplicationPath("/v1")
    static class V1 extends Application {
    }

    @ApplicationPath("/b")
    static class B extends Application {
    }

    @ApplicationPath("/a")
    static class A extends Application {
    }

    private static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> type : classes) {
            indexer.indexClass(type);
        }
        return indexer.complete();
    }

    @Test
    void classicReadsTheResteasyKey() {
        assertEquals("quarkus.resteasy.path", RestPaths.restPathKey(true));
    }

    @Test
    void restReadsTheRestKey() {
        assertEquals("quarkus.rest.path", RestPaths.restPathKey(false));
    }

    @Test
    void applicationPathWins() throws IOException {
        assertEquals("/v1", RestPaths.prefix(index(V1.class), Optional.of("/api")));
    }

    @Test
    void keyApplies() throws IOException {
        assertEquals("/api", RestPaths.prefix(index(), Optional.of("/api/")));
    }

    @Test
    void firstSortedClassWins() throws IOException {
        assertEquals("/a", RestPaths.prefix(index(B.class, A.class), Optional.empty()));
    }
}
