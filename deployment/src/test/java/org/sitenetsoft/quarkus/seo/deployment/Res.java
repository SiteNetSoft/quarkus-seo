package org.sitenetsoft.quarkus.seo.deployment;

import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;
import org.sitenetsoft.quarkus.seo.SeoLocales;
import org.sitenetsoft.quarkus.seo.runtime.SeoSettings;

@Path("/t")
@Produces("text/plain")
public class Res {

    @Inject
    SeoSettings settings;

    @Inject
    SeoLocales locales;

    @Inject
    BaseUrlResolver resolver;

    @GET
    @Path("name")
    public String name() {
        return settings.siteName().orElse("none");
    }

    @GET
    @Path("locales")
    public String locales() {
        return locales.locales().stream().map(Locale::toLanguageTag).toList() + "|"
                + locales.defaultLocale().toLanguageTag();
    }

    @GET
    @Path("indexable")
    public String indexable() {
        return String.valueOf(settings.indexable());
    }

    @GET
    @Path("base")
    public String base(RoutingContext context) {
        return resolver.resolve(context);
    }
}
