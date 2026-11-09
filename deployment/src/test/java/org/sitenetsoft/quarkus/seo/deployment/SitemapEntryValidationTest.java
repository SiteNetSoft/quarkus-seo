package org.sitenetsoft.quarkus.seo.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.sitenetsoft.quarkus.seo.SitemapEntry;

class SitemapEntryValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Bad.class))
            .assertException(t -> assertThat(t)
                    .hasMessageContaining(Bad.class.getName() + "#sometimes")
                    .hasMessageContaining("sometimes")
                    .hasMessageContaining(Bad.class.getName() + "#item")
                    .hasMessageContaining("/items/{id}")
                    .hasMessageContaining(Bad.class.getName() + "#tooHigh")
                    .hasMessageContaining("1.5"));

    @Test
    void invalidEntriesFailTheBuild() {
        fail("The build must fail");
    }

    @Path("/items")
    public static class Bad {

        @GET
        @Path("all")
        @SitemapEntry(changefreq = "sometimes")
        public String sometimes() {
            return "x";
        }

        @GET
        @Path("{id}")
        @SitemapEntry
        public String item(@PathParam("id") String id) {
            return id;
        }

        @GET
        @Path("top")
        @SitemapEntry(priority = 1.5)
        public String tooHigh() {
            return "x";
        }
    }
}
