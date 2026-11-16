package org.sitenetsoft.quarkus.seo.deployment;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;

/**
 * Render {@code templates/tag-page.html}, which calls {@code {#seo title="Home" /}}.
 */
@Path("/tag-page")
@Produces("text/plain")
public class TagPage {

    static final String TEMPLATE = "<head>{#seo title=\"Home\" /}</head>";
    static final String APP_TAG = "APP TAG {title}";

    @Inject
    @Location("tag-page.html")
    Template page;

    @GET
    public String get() {
        return page.render();
    }
}
