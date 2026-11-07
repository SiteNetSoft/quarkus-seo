package org.sitenetsoft.quarkus.seo.it;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import org.sitenetsoft.quarkus.seo.IndexNow;
import org.sitenetsoft.quarkus.seo.Seo;
import org.sitenetsoft.quarkus.seo.SeoCaches;
import org.sitenetsoft.quarkus.seo.SitemapEntry;
import org.sitenetsoft.quarkus.seo.schema.Article;

/**
 * Serve the sample pages and the endpoints the tests use.
 */
@Path("/")
public class PageResource {

    @Inject
    Seo seo;

    @Inject
    Template base;

    @Inject
    SeoCaches caches;

    @Inject
    BlogContributor contributor;

    @Inject
    IndexNow indexNow;

    /**
     * Declare the templates of this resource.
     */
    @CheckedTemplate
    static class Templates {

        private Templates() {
        }

        /**
         * Render a content page.
         *
         * @param slug the page slug
         * @return the template instance
         */
        static native TemplateInstance page(String slug);
    }

    /**
     * Render the home page.
     *
     * @return the page
     */
    @GET
    @Produces("text/html")
    @SitemapEntry(changefreq = "daily", priority = 1.0)
    public TemplateInstance home() {
        return base.instance();
    }

    /**
     * Render a content page with its own SEO values.
     *
     * @param slug the page slug
     * @return the page
     */
    @GET
    @Path("{slug}")
    @Produces("text/html")
    public TemplateInstance page(@PathParam("slug") String slug) {
        String title = Character.toUpperCase(slug.charAt(0)) + slug.substring(1);
        Instant published = Instant.parse("2026-03-01T08:00:00Z");
        CmsPage page = new CmsPage(slug, title, "A page about " + slug, published);
        seo.from(page).type("article").locale(Locale.ENGLISH);
        seo.jsonLd(Article.create()
                .headline(title)
                .description("A page about " + slug)
                .datePublished(published)
                .author("Jane Doe"));
        return Templates.page(slug);
    }

    /**
     * Clear the sitemap cache.
     *
     * @return a confirmation
     */
    @POST
    @Path("invalidate")
    @Produces("text/plain")
    public String invalidate() {
        caches.invalidateSitemap();
        return "ok";
    }

    /**
     * Clear both caches from an event-loop thread.
     *
     * @return a confirmation, or a failure when the method does not run on the event loop
     */
    @POST
    @Path("invalidate-on-event-loop")
    @Produces("text/plain")
    public Uni<String> invalidateOnEventLoop() {
        return Uni.createFrom().item(() -> {
            if (!Context.isOnEventLoopThread()) {
                throw new IllegalStateException("Not on an event-loop thread");
            }
            caches.invalidateSitemap();
            caches.invalidateRobots();
            return "ok";
        });
    }

    /**
     * Report how often the sitemap has asked the blog contributor for URLs.
     *
     * @return the call count
     */
    @GET
    @Path("contributor-calls")
    @Produces("text/plain")
    public String contributorCalls() {
        return Integer.toString(contributor.calls());
    }

    /**
     * Submit a blog URL to IndexNow and wait until the request ends.
     *
     * @return a confirmation
     * @throws InterruptedException when the wait is interrupted
     * @throws ExecutionException when the submission fails
     * @throws TimeoutException when the submission takes longer than ten seconds
     */
    @POST
    @Path("publish")
    @Produces("text/plain")
    public String publish() throws InterruptedException, ExecutionException, TimeoutException {
        indexNow.submit("/blog/first").toCompletableFuture().get(10, TimeUnit.SECONDS);
        return "ok";
    }
}
