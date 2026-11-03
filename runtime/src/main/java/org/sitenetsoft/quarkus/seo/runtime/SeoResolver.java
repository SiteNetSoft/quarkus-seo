package org.sitenetsoft.quarkus.seo.runtime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.seo.ResolvedSeo;
import org.sitenetsoft.quarkus.seo.SeoLocales;
import org.sitenetsoft.quarkus.seo.schema.JsonLd;
import org.sitenetsoft.quarkus.seo.schema.Organization;
import org.sitenetsoft.quarkus.seo.schema.SchemaNode;
import org.sitenetsoft.quarkus.seo.schema.WebSite;

/**
 * Merge the config defaults, the tag values and the Java values into one resolved page record.
 */
@Singleton
public class SeoResolver {

    private static final String TITLE_TOKEN = "%s";

    private final SeoSettings settings;
    private final SeoLocales locales;
    private final SeoConfig config;
    private final SeoBuildConfig buildConfig;

    /**
     * Create the resolver.
     *
     * @param settings the resolved site settings
     * @param locales the site locales
     * @param config the run-time configuration
     * @param buildConfig the build-time configuration
     */
    @Inject
    public SeoResolver(SeoSettings settings, SeoLocales locales, SeoConfig config, SeoBuildConfig buildConfig) {
        this.settings = settings;
        this.locales = locales;
        this.config = config;
        this.buildConfig = buildConfig;
    }

    /**
     * Resolve the values of one page.
     *
     * @param tag the values from the tag parameters
     * @param java the values set in Java
     * @param request the current request, or empty when none is active
     * @return the resolved values
     */
    public ResolvedSeo resolve(SeoValues tag, SeoValues java, Optional<RequestInfo> request) {
        SeoValues defaults = new SeoValues()
                .description(config.description().orElse(null))
                .image(config.image().orElse(null))
                .imageAlt(config.imageAlt().orElse(null));
        SeoValues merged = java.over(tag.over(defaults));
        String base = request.map(RequestInfo::baseUrl).orElse(null);

        String title;
        Optional<String> ogTitle;
        if (merged.absoluteTitle() != null) {
            title = merged.absoluteTitle();
            ogTitle = Optional.of(title);
        } else if (merged.title() != null) {
            title = settings.titleTemplate().replace(TITLE_TOKEN, merged.title());
            ogTitle = Optional.of(merged.title());
        } else {
            title = settings.defaultTitle().orElse("");
            ogTitle = settings.defaultTitle();
        }

        Optional<String> canonical = canonical(merged, request);
        String type = merged.type() == null ? "website" : merged.type();
        Locale pageLocale = merged.locale() == null ? locales.defaultLocale() : merged.locale();

        Optional<ResolvedSeo.Image> image = Optional.empty();
        String imageUrl = absolute(base, merged.image());
        if (imageUrl != null) {
            image = Optional.of(new ResolvedSeo.Image(imageUrl, merged.imageWidth(), merged.imageHeight(),
                    merged.imageAlt()));
        }

        List<String> alternateLocales = new ArrayList<>();
        for (Locale alternate : merged.alternates().keySet()) {
            if (!alternate.equals(pageLocale)) {
                alternateLocales.add(alternate.toString());
            }
        }

        Optional<ResolvedSeo.Article> article = Optional.empty();
        if ("article".equals(type)) {
            article = Optional.of(new ResolvedSeo.Article(Optional.ofNullable(merged.published()),
                    Optional.ofNullable(merged.modified()), Optional.ofNullable(merged.author()),
                    Optional.ofNullable(merged.section()), List.copyOf(merged.tags())));
        }

        String twitterCard = merged.twitterCard() != null ? merged.twitterCard()
                : image.isPresent() ? "summary_large_image" : "summary";

        return new ResolvedSeo(title, Optional.ofNullable(merged.description()), robots(merged), canonical,
                hreflang(merged, pageLocale, canonical, base), type, ogTitle, settings.siteName(),
                pageLocale.toString(), alternateLocales, image, article, twitterCard,
                config.twitter().site(), config.twitter().creator(), verification(),
                config.themeColor(), List.copyOf(merged.extraMeta()), jsonLd(merged, request));
    }

    /**
     * List the robots directives that are set in Java, in output order.
     *
     * @param values the values
     * @return the directives, empty when none are set
     */
    public static List<String> directives(SeoValues values) {
        List<String> result = new ArrayList<>();
        if (Boolean.TRUE.equals(values.noindex())) {
            result.add("noindex");
        }
        if (Boolean.TRUE.equals(values.nofollow())) {
            result.add("nofollow");
        }
        if (Boolean.TRUE.equals(values.noimageindex())) {
            result.add("noimageindex");
        }
        if (values.maxSnippet() != null) {
            result.add("max-snippet:" + values.maxSnippet());
        }
        if (values.maxImagePreview() != null) {
            result.add("max-image-preview:" + values.maxImagePreview());
        }
        if (values.unavailableAfter() != null) {
            result.add("unavailable_after: " + DateTimeFormatter.ISO_INSTANT.format(values.unavailableAfter()));
        }
        return result;
    }

    private List<String> robots(SeoValues merged) {
        if (!settings.indexable()) {
            return List.of("noindex", "nofollow");
        }
        return directives(merged);
    }

    private Optional<String> canonical(SeoValues merged, Optional<RequestInfo> request) {
        if (merged.canonical() != null) {
            return Optional.ofNullable(absolute(request.map(RequestInfo::baseUrl).orElse(null), merged.canonical()));
        }
        if (request.isEmpty()) {
            return Optional.empty();
        }
        RequestInfo info = request.get();
        String url = Urls.absolute(info.baseUrl(), info.path());
        String query = Urls.canonicalQuery(info.rawQuery(), config.canonical().keepQueryParams().orElse(List.of()));
        return Optional.of(query.isEmpty() ? url : url + "?" + query);
    }

    private List<ResolvedSeo.Link> hreflang(SeoValues merged, Locale pageLocale, Optional<String> canonical,
            String base) {
        if (merged.alternates().isEmpty() || canonical.isEmpty()) {
            return List.of();
        }
        List<ResolvedSeo.Link> links = new ArrayList<>();
        links.add(new ResolvedSeo.Link(pageLocale.toLanguageTag(), canonical.get()));
        for (Map.Entry<Locale, String> entry : merged.alternates().entrySet()) {
            String href = absolute(base, entry.getValue());
            if (href != null && !entry.getKey().equals(pageLocale)) {
                links.add(new ResolvedSeo.Link(entry.getKey().toLanguageTag(), href));
            }
        }
        String defaultTag = locales.defaultLocale().toLanguageTag();
        for (ResolvedSeo.Link link : List.copyOf(links)) {
            if (link.hreflang().equals(defaultTag)) {
                links.add(new ResolvedSeo.Link("x-default", link.href()));
                break;
            }
        }
        return links;
    }

    private List<ResolvedSeo.Meta> verification() {
        List<ResolvedSeo.Meta> result = new ArrayList<>();
        config.verification().google()
                .ifPresent(value -> result.add(new ResolvedSeo.Meta("name", "google-site-verification", value)));
        config.verification().bing()
                .ifPresent(value -> result.add(new ResolvedSeo.Meta("name", "msvalidate.01", value)));
        config.verification().yandex()
                .ifPresent(value -> result.add(new ResolvedSeo.Meta("name", "yandex-verification", value)));
        return result;
    }

    private String jsonLd(SeoValues merged, Optional<RequestInfo> request) {
        if (!buildConfig.jsonLd().enabled()) {
            return "";
        }
        List<SchemaNode> nodes = new ArrayList<>();
        if (request.isPresent() && request.get().home()) {
            addSiteNodes(nodes, request.get().baseUrl());
        }
        nodes.addAll(merged.jsonLdNodes());
        return JsonLd.graph(nodes, merged.jsonLdRaw());
    }

    private void addSiteNodes(List<SchemaNode> nodes, String base) {
        Organization organization = null;
        SeoConfig.Organization orgConfig = config.organization();
        if (orgConfig.enabled() && orgConfig.name().isPresent()) {
            organization = Organization.create()
                    .id(base + "/#organization")
                    .name(orgConfig.name().get())
                    .url(orgConfig.url().orElse(base + "/"))
                    .logo(orgConfig.logo().map(logo -> Urls.absolute(base, logo)).orElse(null))
                    .sameAs(orgConfig.sameAs().orElse(null));
            nodes.add(organization);
        }
        if (config.website().enabled() && settings.siteName().isPresent()) {
            nodes.add(WebSite.create()
                    .id(base + "/#website")
                    .name(settings.siteName().get())
                    .url(base + "/")
                    .publisher(organization));
        }
    }

    private static String absolute(String base, String value) {
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return value;
        }
        return base == null ? null : Urls.absolute(base, value);
    }
}
