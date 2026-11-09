package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import org.sitenetsoft.quarkus.seo.SitemapEntry;

class SitemapEntryScannerTest {

    @Test
    void joinsClassAndMethodPaths() throws IOException {
        List<SitemapEntryBuildItem> entries = SitemapEntryScanner.scan(
                Index.of(Joined.class, ClassOnly.class, Root.class, NotGet.class, NoClassPath.class), "");
        assertThat(entries).extracting(SitemapEntryBuildItem::path)
                .containsExactlyInAnyOrder("/a/b", "/only", "/", "/c/d");
    }

    @Test
    void keepsChangefreqAndPriority() throws IOException {
        List<SitemapEntryBuildItem> entries = SitemapEntryScanner.scan(Index.of(Root.class), "");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).changefreq()).isEqualTo("daily");
        assertThat(entries.get(0).priority()).isEqualTo(0.8);
    }

    @Test
    void leavesUnsetValuesUnset() throws IOException {
        List<SitemapEntryBuildItem> entries = SitemapEntryScanner.scan(Index.of(ClassOnly.class), "");
        assertThat(entries.get(0).changefreq()).isEmpty();
        assertThat(entries.get(0).priority()).isEqualTo(-1);
    }

    @Test
    void addsTheRestPathPrefix() throws IOException {
        String prefix = RestPaths.prefix(Index.of(Joined.class), Optional.of("/api/"));
        assertThat(prefix).isEqualTo("/api");
        assertThat(SitemapEntryScanner.scan(Index.of(Joined.class, Root.class), prefix))
                .extracting(SitemapEntryBuildItem::path).containsExactly("/api", "/api/a/b");
    }

    @Test
    void applicationPathWinsOverTheRestPath() throws IOException {
        Index index = Index.of(Joined.class, App.class);
        String prefix = RestPaths.prefix(index, Optional.of("/api"));
        assertThat(prefix).isEqualTo("/shop");
        assertThat(SitemapEntryScanner.scan(index, prefix)).extracting(SitemapEntryBuildItem::path)
                .containsExactly("/shop/a/b");
    }

    @Test
    void rootPrefixIsEmpty() throws IOException {
        assertThat(RestPaths.prefix(Index.of(Joined.class), Optional.empty())).isEmpty();
        assertThat(RestPaths.prefix(Index.of(Joined.class), Optional.of("/"))).isEmpty();
    }

    @ApplicationPath("shop/")
    public static class App extends Application {
    }

    @Path("/a")
    public static class Joined {

        @GET
        @Path("b/")
        @SitemapEntry
        public String b() {
            return "b";
        }
    }

    @Path("only/")
    public static class ClassOnly {

        @GET
        @SitemapEntry
        public String page() {
            return "only";
        }
    }

    @Path("/")
    public static class Root {

        @GET
        @SitemapEntry(changefreq = "daily", priority = 0.8)
        public String home() {
            return "home";
        }
    }

    @Path("/c")
    public static class NotGet {

        @POST
        @Path("/ignored")
        @SitemapEntry
        public String post() {
            return "post";
        }

        @GET
        @Path("d")
        @SitemapEntry
        public String get() {
            return "get";
        }
    }

    public static class NoClassPath {

        @GET
        @Path("/lost")
        @SitemapEntry
        public String lost() {
            return "lost";
        }
    }
}
