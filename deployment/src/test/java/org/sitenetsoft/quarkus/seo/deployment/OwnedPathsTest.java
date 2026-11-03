package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;

class OwnedPathsTest {

    @Test
    void joinsClassAndMethodPaths() throws IOException {
        assertThat(OwnedPaths.fromIndex(Index.of(Joined.class))).containsExactlyInAnyOrder("/a", "/a/b");
    }

    @Test
    void joinsRouteBaseAndRoutePaths() throws IOException {
        assertThat(OwnedPaths.fromIndex(Index.of(Routes.class))).containsExactlyInAnyOrder("/base/r", "/base/q", "/base/x",
                "/base/y");
    }

    @Test
    void skipsInterfaces() throws IOException {
        assertThat(OwnedPaths.fromIndex(Index.of(Client.class))).isEmpty();
    }

    @Test
    void findsStaticFiles() {
        StaticResourcesBuildItem statics = new StaticResourcesBuildItem(
                Set.of(new StaticResourcesBuildItem.Entry("/robots.txt", false)));
        assertThat(OwnedPaths.ownedStatic(Optional.of(statics), "/robots.txt")).isTrue();
        assertThat(OwnedPaths.ownedStatic(Optional.of(statics), "/sitemap.xml")).isFalse();
        assertThat(OwnedPaths.ownedStatic(Optional.empty(), "/robots.txt")).isFalse();
    }

    @Path("/a")
    public static class Joined {

        @GET
        @Path("b/")
        public String b() {
            return "b";
        }
    }

    @RouteBase(path = "base")
    public static class Routes {

        @Route(path = "r")
        public String r() {
            return "r";
        }

        @Route(path = "/q/")
        public String q() {
            return "q";
        }

        @Route(path = "x")
        @Route(path = "y")
        public String xy() {
            return "xy";
        }
    }

    @Path("/client")
    public interface Client {

        @GET
        String get();
    }
}
