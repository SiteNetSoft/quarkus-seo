# Quarkus SEO Design

**Status:** implemented (plans 1 and 2)
**Date:** 2026-10-29
**Deliverable:** a Quarkus extension, `org.sitenetsoft:quarkus-seo`, built with Maven. It gives an
application meta tags, JSON-LD, a sitemap and robots.txt with almost no setup. It also offers
security.txt, a web manifest, IndexNow and llms.txt as opt-in features.

## 1. Purpose and intent

SEO output today is hand-written in each application. The current example is
`platform-website` (`/home/jn/Projects/SiteNetSoft-private/platform-website`). There, a
hard-coded slug list builds the sitemap, a fixed string is the robots.txt, and an 8-field record
feeds the `<head>`. The controller passes that record to the template by hand on every page.
Several bugs come from this approach (see §13).

No Quarkus extension covers this. Quarkus Roq has a sitemap plugin and a `{#seo}` tag, but only
for static sites. Nothing serves dynamic, database-backed applications.

**Intent:** a developer adds `quarkus-seo`, sets the site URL, and puts `{#seo /}` in the
`<head>` of their base template. The page then gets correct title, description, canonical,
hreflang, Open Graph, Twitter and robots tags. `/sitemap.xml` and `/robots.txt` work without
code. For pages built from data, the request handler sets values on an injected `Seo` object.
For simple pages, the template sets them on the tag. Every built-in part can be configured,
replaced or switched off.

`platform-website` will move its SEO code to this extension later. That move makes the
microservice smaller and fixes the bugs in §13. The move itself is not part of this repo.

### Goals

- Meta tags from one tag, `{#seo /}`, with values from config, Java and the template.
- A sitemap that comes from config, from annotated endpoints and from developer beans. It splits
  into an index above 50,000 URLs.
- A robots.txt from config, with a `Sitemap:` line and presets for AI crawlers.
- Automatic non-indexing outside the prod profile.
- JSON-LD with typed schema.org builders.
- Opt-in security.txt, web manifest, IndexNow and llms.txt.
- Caching that the developer can switch on, using `quarkus-cache`.
- Native image support.
- A Dev UI page for preview and content checks.

### Non-goals

- Generating Open Graph images.
- Broken-link checking.
- Sitemap ping. Google removed the ping endpoint in 2023.
- News and video sitemap extensions. A later version can add them.
- RSS and Atom feeds.
- humans.txt. A developer can ship it as a static file.
- `<meta charset>`, `viewport` and favicon links. These are page layout, not SEO. Layout
  libraries such as quarkus-pha own them.
- Full JSON-LD processing such as expansion, compaction or signatures. That belongs to a future
  `quarkus-jsonld`.
- Migrating `platform-website`.

### Anti-goals (what would make this a failure even if it works)

- **Noise.** The extension must not fail startup because of missing SEO config. It must not log
  warnings in prod. It always falls back to a sensible value.
- **Forced dependencies.** It must not depend on other SiteNetSoft extensions. It must not pull in
  Jackson or `quarkus-cache`.
- **Wrong canonical URLs.** A spoofed `Host` header must not end up in canonical links.
- **An indexed staging site.** Dev and test profiles must not be indexable by default.
- **Conflicts with the application.** If the application ships its own robots.txt or sitemap, the
  extension must not serve a second one.
- **Lock-in.** A developer must be able to replace any part without forking the extension.

## 2. Constraints

- **Quarkus 3.40.1. Maven compiler release 21.** The build runs on JDK 25, like
  quarkus-microformats.
- **Build files:** the root POM, release profile, CI and docs module are copied from
  quarkus-microformats (`/home/jn/Projects/SiteNetSoft/quarkus-microformats`).
- **Coordinates and packages:** groupId `org.sitenetsoft`. The artifacts are `quarkus-seo` and
  `quarkus-seo-deployment`. Packages:
  - `org.sitenetsoft.quarkus.seo` holds the public API.
  - `.schema` holds the schema.org builders and the JSON-LD writer (see §6). It uses only the
    JDK.
  - `.runtime` holds the internals.
  - `.deployment` holds the build steps.
- **Prose and Javadoc rules:** the same as quarkus-microformats. Prose follows the Simplified
  Technical English rules of quarkusio/quarkus PR 57821. Javadoc follows PR 57819. Both are listed
  in quarkus-microformats `.superpowers/sdd/2026-10-26-quarkus-microformats-parser/global-constraints.md`.
- **Git attribution:** follow `/home/jn/Projects/SiteNetSoft/ai-skills/dev-practices/git/git-authorship.md`.
  A commit message is one imperative sentence. It has no trailers and does not mention AI or
  tooling.
- **Host build limits** (from the user's global instructions):
  - Native builds pass `-J-XX:TieredStopAtLevel=1,--parallelism=8`.
  - Maven runs with `-T 8` at most. quarkus-microformats CI uses `-T 4`.
  - Only one heavy build runs at a time.
- **No compile-time dependency on other SiteNetSoft extensions.** Integration with other
  extensions uses Quarkus capabilities (§11).

## 3. Module layout

| Module | artifactId | Published | Role |
|---|---|---|---|
| `runtime/` | `quarkus-seo` | yes | API, config, routes, Qute tag, renderers |
| `deployment/` | `quarkus-seo-deployment` | yes | Build steps, conflict detection, Dev UI |
| `integration-tests/` | – | no | A sample application, JVM and native tests |
| `docs/` | – | no | Antora-structured docs, built with Maven asciidoctor |

**Runtime dependencies:** `quarkus-arc`, `quarkus-vertx-http` and `quarkus-qute`. The designer chose a hard Qute
dependency to make Qute support always available, because the Qute tag
is the main way to use the extension. `quarkus-cache` is optional and is used only when the
application has it.

The runtime descriptor (`META-INF/quarkus-extension.yaml`) declares:
- the capability `org.sitenetsoft.seo`, which the extension provides only when `quarkus.seo.enabled` and
  `quarkus.seo.meta.enabled` are both true
- `config: ["quarkus.seo."]`
- `categories: ["web"]`
- `status: "preview"`
- a description that starts with a verb, e.g. "Add meta tags, sitemaps and robots.txt to web pages"

## 4. Configuration

### 4.1 Principle: own keys, Quarkus fallbacks

Each value that Quarkus already has gets its own `quarkus.seo.*` key. When the SEO key is not
set, the Quarkus value is used. Quarkus values can mean something else in some applications.
`quarkus.application.name` is one example. So the developer must be able to override them for
SEO only.

| SEO key | Falls back to | Final fallback |
|---|---|---|
| `quarkus.seo.site-name` | `quarkus.application.name` | no site name; site-name tags are left out |
| `quarkus.seo.default-locale` | `quarkus.default-locale` | first entry of the locale list |
| `quarkus.seo.locales` | `quarkus.locales` | `en-US` |
| `quarkus.seo.site-url` | none | the request host (§4.3) |

Other Quarkus config is used as it is, with no SEO key:
- `quarkus.http.root-path` is the prefix for endpoints and generated links.
- `quarkus.http.proxy.*` controls which forwarded headers are trusted.
- `quarkus.http.enable-compression` and `compress-media-types` control gzip of the sitemap.
- `quarkus.http.filter.*` adds extra headers per path.
- The `prod` profile sets the default for `indexable`.
- The `quarkus.cache.*` settings for a named cache.

### 4.2 Main keys

All keys are run-time keys, unless they are marked "build time". A disabled feature is not
registered at all, so its switch is a build-time key. Every output path is also a build-time
key: `sitemap.path`, `sitemap.page-path`, `manifest.path` and `indexnow.key`. This lets the
conflict check in §10 see them. Changing a path needs a rebuild. This is the designer's call.

| Key | Default | Meaning |
|---|---|---|
| `quarkus.seo.enabled` (build time) | `true` | Master switch. `false` turns every feature off. |
| `quarkus.seo.indexable` | `true` in the prod profile, `false` otherwise | Lets search engines index the site. |
| `quarkus.seo.allowed-hosts` | empty | Hosts accepted for absolute URLs (§4.3). |
| `quarkus.seo.title.template` | `%s \| {site-name}`, or `%s` when there is no site name | The pattern for the page title. |
| `quarkus.seo.title.default` | the site name | The title when a page sets none. |
| `quarkus.seo.description` | none | The default description. |
| `quarkus.seo.image` | none | The default Open Graph image, relative or absolute. |
| `quarkus.seo.image-alt` | none | The default image alt text. |
| `quarkus.seo.twitter.site`, `.creator` | none | `twitter:site` and `twitter:creator`. |
| `quarkus.seo.verification.google`, `.bing`, `.yandex` | none | The site verification meta tags. |
| `quarkus.seo.theme-color` | none | `theme-color`. |
| `quarkus.seo.canonical.keep-query-params` | empty | The query parameters kept in canonical URLs. All others are removed. |
| `quarkus.seo.meta.enabled` (build time) | `true` | The `{#seo}` tag, the `Seo` bean and the X-Robots-Tag filter. |
| `quarkus.seo.json-ld.enabled` (build time) | `true` | JSON-LD output in `{#seo}`. |
| `quarkus.seo.organization.*` | none | Site-wide Organization JSON-LD: name, url, logo, same-as list. |

Keys for the sitemap, robots.txt and the opt-in features are listed in their own sections.

### 4.3 Site URL and host

Absolute URLs (canonical, `og:url`, `og:image`, hreflang, sitemap `loc`) need a base. The
extension resolves it in this order:

1. `quarkus.seo.site-url`, if set.
2. Otherwise, the scheme and host of the current request as Quarkus resolved them. Quarkus uses
   forwarded headers only when the developer enabled `quarkus.http.proxy.*`. Otherwise it uses
   the plain `Host` header. The extension does not read forwarded headers itself.
3. If `quarkus.seo.allowed-hosts` is set and the resolved host is not in it, the first allowed
   host is used.

The proxy in front of the application may be unknown. That is why the extension never fails
when no site URL is set. The risk is a spoofed `Host` value in canonical links, which a CDN can
then cache. The docs must explain this risk and recommend `site-url` or `allowed-hosts` in prod.
A sitemap or robots.txt request uses the same rules.

### 4.4 Locales

The locale list comes from a bean, not straight from config. The interface is `SeoLocales`, with
`List<Locale> locales()` and `Locale defaultLocale()`. The default bean is `@DefaultBean` and
reads the config in §4.1. An application whose languages change at run time, for example from a
database, provides its own bean. The sitemap, hreflang and `og:locale` then follow it.

## 5. Page metadata

### 5.1 The `Seo` object

`Seo` is a `@RequestScoped` bean. A request handler injects it and sets values with a fluent
API. Example:

```java
@Inject Seo seo;

@GET @Path("/{slug}")
public TemplateInstance page(String slug) {
    CmsPage page = cms.get(slug);
    seo.title(page.title())
       .description(page.summary())
       .image(page.heroImage())
       .modified(page.updatedAt())
       .locale(Locale.FRENCH)
       .alternate(Locale.ENGLISH, "/en/" + page.enSlug());
    return pageTemplate.data("page", page);
}
```

The fields:
- title, and absolute title (no template)
- description
- canonical (relative or absolute)
- type (`website`, `article`, ...)
- image with width, height and alt
- locale and alternates (locale to path)
- robots directives (`noindex`, `nofollow`, `noimageindex`, `max-snippet`, `max-image-preview`,
  `unavailable-after`)
- published and modified times, author, section, tags (for `article:*`)
- Twitter card type
- JSON-LD nodes
- extra meta tags, as an escape hatch

`seo.from(SeoSource)` reads the values from an object that implements the small `SeoSource`
interface. Every method of that interface is a default method that returns an empty `Optional`,
so a page object implements only what it has.

### 5.2 Merge order

The resolved value of each field comes from, lowest first:

1. config defaults (§4.2)
2. parameters on the `{#seo}` tag
3. values set on `Seo` in Java

Java wins over the tag. The tag often sits in a shared layout, such as pha's `_head.html`, and
passes the same values for every page. A value set in Java belongs to one request, so it is
the more specific one. An empty or null value, on the tag or in Java, counts as not set. This is
the designer's call.

A higher layer replaces only the fields it sets. Setting a title does not clear the image.
Lists such as alternates and JSON-LD nodes add to the lower layer. They do not replace it.

Some fields merge as a group. The title and the absolute title are one group. The image, its size and its alt text
are another. A layer that sets any field of a group replaces the whole group of the lower layers.

### 5.3 Computed values

- **Title:** the template is applied to the page title. With no page title, `title.default` is
  used and the template is not applied. An absolute title skips the template.
- **Canonical:** the default is the base URL plus the request path, with every query parameter
  removed except the ones in `canonical.keep-query-params`. The path is kept as requested. A
  relative canonical value is made absolute.
- **Absolute URLs:** relative image and alternate paths are made absolute with the base URL.
- **`og:locale`:** the page locale, or the default locale. `og:locale:alternate` comes from the
  alternates.
- **hreflang:** one link per alternate, a link for the page itself, and `x-default` pointing to
  the default-locale alternate. Links are written only when the page has at least one alternate.
- **Twitter:** `twitter:card` is `summary_large_image` when there is an image, and `summary`
  otherwise. Title, description and image are not repeated, because X falls back to Open Graph.
- **Robots:** when `indexable` is false, every page gets `noindex, nofollow`. Otherwise only the
  directives set on the page are written.

### 5.4 Qute

`{#seo /}` is a section helper registered by the extension. It renders all head tags for the
current request. Parameters set values for this page, for example:

```html
<head>
  <meta charset="utf-8">
  {#seo title="About us" description="Who we are" /}
</head>
```

Parameter names match the `Seo` fields (`title`, `description`, `image`, `canonical`, `type`,
`noindex`, ...). Values can be any Qute expression, including message bundle keys such as
`msg:about_title`. This is how translated titles work; the extension has no translation code of
its own.

A `seo:` namespace gives access to single resolved values for use in the page body, for example
`{seo:title}` or `{seo:canonical}`.

Output rules:
- A tag with no value is not written. Empty `content=""` attributes never appear.
- All attribute values are HTML-escaped.
- JSON-LD escapes `</` so content cannot close the script tag.
- Tags are written in a fixed order, one per line.

### 5.5 Java rendering

`SeoHeadRenderer` renders the same output as a string, for applications that do not use Qute or
that build their head another way. `{#seo}` calls it. The default bean is `@DefaultBean`, so a
developer can replace it.

### 5.6 X-Robots-Tag

A response filter writes `X-Robots-Tag` when:
- `indexable` is false (on every response, including non-HTML files), or
- the page set robots directives on `Seo`.

The header carries the directives known before the body is written: the `indexable` rule and
the directives set in Java. Directives set only on the `{#seo}` tag go into the meta tag, not the
header, because the headers can already be sent by then. The docs tell developers to set
`noindex` in Java when they also need the header. The extension does not buffer response bodies.

## 6. JSON-LD

The extension writes JSON-LD itself. It does not depend on Jackson or on a JSON-LD processor,
because it only writes schema.org output and never reads or expands it.

- **Typed builders:** `Organization`, `WebSite`, `BreadcrumbList` and `Article`, plus a generic
  `SchemaNode.of("Product")` builder for any other type.
- **Raw JSON:** `seo.jsonLdRaw(String)` takes JSON the developer already has. The extension
  embeds it with `</` escaping and does not parse it.
- **Output:** one `<script type="application/ld+json">` with `@context: https://schema.org`
  and a `@graph`. Nodes link with `@id` values built from the base URL.
- **Site-wide nodes:** `Organization` comes from `quarkus.seo.organization.*`, and `WebSite`
  comes from the site name and URL. Both are added on the home page only (path `/` under the
  root path). The developer can switch each one off.
- **No SearchAction.** Google retired the sitelinks search box.
- **Self-contained package:** the builders, `SchemaNode` and the JSON writer are in
  `org.sitenetsoft.quarkus.seo.schema`. This package uses only the JDK. It does not import
  Quarkus, CDI, Qute, Vert.x or other quarkus-seo packages, and the other packages depend on it,
  not the reverse. A build test checks this rule. If quarkus-jsonld or another project needs
  the builders later, the package can move to its own small library without changes to its
  code.

## 7. Sitemap

### 7.1 Sources

The sitemap combines three sources:

1. **Config:** `quarkus.seo.sitemap.urls` is a list of paths.
2. **Annotation:** `@SitemapEntry` on a JAX-RS `GET` method whose path has no template
   variables. A build step finds these in the Jandex index. The annotation can set `changefreq`
   and `priority`.
3. **Beans:** each CDI bean that implements `SitemapContributor` provides
   `Stream<SitemapUrl> urls()`. The stream is read on a worker thread and closed after use, so
   a contributor can stream from a database cursor.

`SitemapUrl` holds:
- `loc` (relative or absolute)
- `lastmod` (optional)
- `changefreq` and `priority` (optional, left out unless set)
- alternates (locale to path)
- image URLs
- `title` (optional, used only by llms.txt)

Google ignores `changefreq` and `priority` and trusts `lastmod` only when it is accurate. The
docs must say so. The extension never sets `lastmod` to the current time.

### 7.2 Output

- **Paths:** `quarkus.seo.sitemap.path`, default `/sitemap.xml`.
- **Small sitemaps:** up to `quarkus.seo.sitemap.max-urls` URLs (default 50,000) are served as one
  `urlset` at that path.
- **Large sitemaps:** above that limit, the path serves a `sitemapindex`. Pages are served at
  `quarkus.seo.sitemap.page-path`, default `/sitemap-{n}.xml`.
- **hreflang:** alternates are written as `xhtml:link` elements, with the self link and
  `x-default`. Images are written with the image namespace.
- **Content type:** `application/xml; charset=utf-8`.
- **ETag:** always sent, and a matching request gets a 304.
- **Cache-Control:** from `quarkus.seo.sitemap.cache-control`, default `public, max-age=3600`.
- **gzip:** comes from Quarkus HTTP compression when the developer enables it.

### 7.3 Caching

Caching is opt-in.
- With `quarkus.seo.sitemap.cache.enabled=true` and `quarkus-cache` present, the generated XML is
  stored in the named cache `seo-sitemap`. The developer configures it with
  `quarkus.cache.caffeine.seo-sitemap.*` or the Redis and Infinispan equivalents.
- `SeoCaches.invalidateSitemap()` clears it, for example after a CMS publish.
- If caching is enabled without `quarkus-cache`, the extension logs one info message at startup
  and serves without a cache.
- The output cache is used only when `site-url` or `allowed-hosts` pins the host. Without a pinned host, the output
  depends on the request host, so the extension serves without a cache.

The robots.txt output uses the same pattern with the cache `seo-robots`. The llms.txt output has no output
cache. Its ETag and Cache-Control header still apply.

### 7.4 Switches

- `quarkus.seo.sitemap.enabled` (build time) is `true` by default.
- When `indexable` is false, the sitemap is still served, but robots.txt does not list it.

## 8. robots.txt

- **Path:** always `/robots.txt`, because RFC 9309 says crawlers read only that path. The path
  cannot be configured.
- **Root path:** see §8.1.
- **Rules:** come from config groups:
  ```properties
  quarkus.seo.robots.groups.all.user-agents=*
  quarkus.seo.robots.groups.all.disallow=/account,/admin
  quarkus.seo.robots.groups.all.allow=/
  quarkus.seo.robots.groups.all.crawl-delay=5
  ```
  With no groups, the output is `User-agent: *` and `Allow: /`.
- **AI preset:** `quarkus.seo.robots.ai`, default `allow`. The values are:
  - `allow`: adds nothing.
  - `block-training`: disallows the training crawlers (`GPTBot`, `ClaudeBot`, `Google-Extended`,
    `Applebot-Extended`, `CCBot`, `Bytespider`, `Meta-ExternalAgent`, `cohere-ai`, `Amazonbot`)
    and keeps the search and user-request crawlers.
  - `block-all`: also disallows `OAI-SearchBot`, `ChatGPT-User`, `Claude-SearchBot`,
    `Claude-User`, `PerplexityBot` and `Perplexity-User`.
  The token lists live in one runtime class, so a release can update them.
  `quarkus.seo.robots.ai-agents.training` and `.search` replace the lists from config.
- **Sitemap line:** a `Sitemap:` line with the absolute sitemap URL is added when the sitemap is
  enabled and the site is indexable.
- **Not indexable:** the output is `User-agent: *` and `Disallow: /`, whatever the other config
  says.
- **Customizing:** a bean that implements `RobotsCustomizer` can edit the built rules in Java. A
  `@DefaultBean` `RobotsTxtProvider` can be replaced to write the whole file.
- **Headers:** `text/plain; charset=utf-8`, with an ETag and the same Cache-Control setting as
  the sitemap.

### 8.1 Host-root files under a root path

Four outputs only work at the root of the host:
- robots.txt
- `/.well-known/security.txt`
- the IndexNow key file
- llms.txt

When `quarkus.http.root-path` is `/`, nothing special happens. When it is not `/`, all four follow
one rule. This is the designer's call.

1. If the extension can register a route at the host root (spike 2 in §17), it serves the file
   there, as usual.
2. If it cannot, it does not serve the file at all. Under the root path a crawler would never
   read it. The extension logs the reason once at debug level, and the Dev UI shows it.
3. The developer can force the file under the root path with
   `quarkus.seo.<feature>.serve-under-root-path=true`. This is for setups where a proxy maps the
   host root to the application.

This replaces the "only if explicitly enabled" idea, so every `enabled` switch stays a plain
boolean.

## 9. Opt-in features

All of these are off by default. Each one has a build-time `enabled` key.

- **security.txt** (`quarkus.seo.security-txt.*`):
  - Served at `/.well-known/security.txt`. The path is fixed.
  - Keys: `contact` (list), `expires`, `encryption`, `policy`, `preferred-languages`, `hiring`.
    `Canonical` is added automatically.
  - When `expires` is in the past or less than 30 days away, the Dev UI and dev mode show a
    warning. A date equal to now counts as expired. Prod stays quiet.
  - `expires` accepts an ISO offset date-time or a plain date, which means the start of that day in UTC. A
    date-time with no offset is dropped. A missing or unparseable value leaves the line out, and the response is
    still 200. The Dev UI and the dev log warn.
  - A contact that holds `@` and no scheme gets `mailto:`. With no contact, the path answers 404.
  - The file is served at the host root, like robots.txt. `serve-under-root-path` (build time, default `false`)
    moves it under the root path. The IndexNow key file and llms.txt follow the same rule (§8.1).
- **Web manifest** (`quarkus.seo.manifest.*`):
  - Served at `quarkus.seo.manifest.path`, default `/manifest.webmanifest`, with
    `application/manifest+json`.
  - Keys: `name` (falls back to the site name), `short-name`, `start-url`, `display`,
    `theme-color`, `background-color` and `icons` (src, sizes, type).
  - `start-url` defaults to the root path plus `/`. A `start-url` or icon `src` that starts with `/` gets the root
    path in front. The manifest sits under the root path, like the sitemap.
  - `{#seo}` adds the `<link rel="manifest">` tag only when the extension serves the manifest. An application
    that ships its own manifest writes its own link.
- **IndexNow** (`quarkus.seo.indexnow.*`):
  - Keys: `key` (8 to 128 characters of `[a-zA-Z0-9-]`) and `endpoint` (default
    `https://api.indexnow.org/indexnow`).
  - When the feature is on and the key is missing or invalid, the build fails. The message names the key.
  - The key file is served at `/{key}.txt`.
  - The `IndexNow` bean has `submit(String... paths)` and `submit(Collection<String>)`. It posts
    up to 10,000 URLs per call with the JDK `HttpClient`, off the calling thread.
  - Failures are logged at debug level and never thrown to the caller.
  - When `indexable` is false, `submit` does nothing. It needs `quarkus.seo.site-url` too, because a
    background job has no request to take a host from. Without `site-url`, it does nothing and logs at DEBUG.
    A URL on another host is dropped. The returned stage always completes normally.
  - When the feature is on, the `IndexNow` bean exists even if the application serves its own key file.
    Ownership skips only the key-file route.
- **llms.txt** (`quarkus.seo.llms-txt.*`):
  - Served at `/llms.txt`, generated from the sitemap sources: the site name as the heading, the
    site description as a quote, then one link per URL that has a title. With no site name, the heading is the
    base URL.
  - The file is served when the site is not indexable, like the sitemap. It has no output cache.
  - The docs say plainly that no search engine is known to use this file.

## 10. Conflicts and switching off

- **Master switch:** `quarkus.seo.enabled=false` turns every feature off.
- **Feature switches:** each feature has its own build-time switch. A disabled feature registers
  no route, bean or filter.
- **Application files win.** At build time the extension checks for its paths among:
  - static resources (`META-INF/resources/robots.txt` and the others)
  - JAX-RS resource paths, with the `@ApplicationPath` or `quarkus.rest.path` prefix
  - Reactive Routes paths

  If the application has any of them, the extension does not register its own route for that
  path. It logs nothing in prod, and the Dev UI shows which outputs the application owns.
- **Sitemap entry paths:** the same JAX-RS prefix applies to the paths of `@SitemapEntry` methods.
- **Routes:** the extension registers plain Vert.x routes through `RouteBuildItem`, not JAX-RS
  resources. JAX-RS needs a constant `@Path`, which is why quarkus-tus cannot move its endpoints.
  Vert.x routes let the sitemap and manifest paths come from config.
- **Replaceable beans:** every service is a `@DefaultBean`:
  - `SeoLocales`
  - `SeoHeadRenderer`
  - `SitemapWriter`
  - `RobotsTxtProvider`
  - `BaseUrlResolver`
  - `IndexNow`

## 11. Integration with other extensions

There are no compile-time dependencies between SiteNetSoft extensions. Integration uses Quarkus
capabilities:

1. quarkus-seo declares the capability `org.sitenetsoft.seo`.
2. Another extension checks it in its deployment processor with
   `Capabilities.isPresent("org.sitenetsoft.seo")`. That is a string check, with no dependency on
   quarkus-seo.
3. The extension that knows the other one does the integration. quarkus-seo knows nothing about
   them.

The public contract that other extensions may rely on is:
- the `{#seo}` tag and its parameter names
- the `Seo` bean API
- the capability name

Breaking changes to these need a major version.

**quarkus-pha** is the first user (that work happens in the quarkus-pha repo, not here). When the
capability is present, pha's `layouts/_head.html` renders `{#seo /}` in place of its own title and
description. Pha's page title and `applicationPageDescription` go to `{#seo}` as parameters, so
nothing is lost. `quarkus.pha.seo-integration=false` keeps pha's own head.

**quarkus-jsonld** does not exist yet. It is only a row in the OpenWebExt architecture spec. It
will be a JSON-LD processor built on Titanium. quarkus-seo does not wait for it or depend on it.
The two do not do the same job:

- quarkus-seo writes schema.org JSON-LD into a page. It never reads or expands JSON-LD.
- quarkus-jsonld will read and process JSON-LD: expansion, compaction and Linked Data signatures.

The schema.org builders are only in quarkus-seo. If quarkus-jsonld needs them, the
`.schema` package (§6) moves to a shared library that both use. The OpenWebExt architecture
spec states the same split.

## 12. Dev UI

The designer put the Dev UI in version 1. The user can move it to a later version. It is
only active in dev mode and has:
- **Overview:** the resolved values (site URL, site name, locales, indexable, enabled features),
  and which outputs the application owns (§10).
- **Preview:** links to the sitemap, robots.txt and the other outputs.
- **Page check:** an input that takes a path. It renders the resolved head tags, shows a
  search-result snippet and social card preview, and lists content warnings:
  - missing title or description
  - title over 60 characters, or description outside 50 to 160 characters
  - relative or missing `og:image`
  - one-way hreflang
  - duplicate titles across sitemap URLs

The page check fetches only `http://localhost:{quarkus.http.port}` plus the root path. It refuses any input that
is not a plain local path and never follows redirects. It checks up to 20 hreflang targets and up to 200 sitemap
URLs, and counts the rest as skipped. Same-site matching uses `site-url`, or localhost when `site-url` is unset.
One-way hreflang compares hrefs as exact strings. A page that renders no `{#seo}` tag shows "no SEO head found",
because the check reads the head that `SeoDevCapture` recorded and does not parse HTML.

The overview warns when the security.txt expiry is missing, invalid, in the past or less than 30 days ahead.

The same warnings are logged in dev mode, once per path. They are never logged in prod.

## 13. platform-website: what the design fixes

These are the gaps found in `platform-website` today. Each is fixed by a part of this design.

| Today | Fixed by |
|---|---|
| Most non-CMS pages use the site root as canonical and say "coming soon" | Canonical from the request path (§5.3) |
| hreflang links are relative paths | Absolute alternates (§5.3) |
| The sitemap is a hard-coded list and misses `/products`, `/contact` and the `/fr`, `/es` pages | Annotation and contributor sources, with alternates (§7.1) |
| `og:locale` is fixed in config | Taken from the page locale (§5.3) |
| robots.txt allows everything, even in placeholder mode | Rule groups and `indexable` (§8) |
| No cache headers on the sitemap or robots.txt | ETag and Cache-Control (§7.2) |
| Two different defaults for the site URL | One base URL resolver (§4.3) |
| SEO data is passed to the template by hand | The `Seo` bean and `{#seo}` (§5) |
| The `/{slug}` route needs a guard for `sitemap.xml` and `robots.txt` | Routes registered by the extension. The guard remains the application's job. |

## 14. Testing

- **Unit tests** (runtime, JUnit 5 and AssertJ):
  - merge order
  - title template
  - canonical rules
  - escaping
  - JSON-LD output
  - sitemap XML and splitting
  - robots output for each preset
  - security.txt and manifest output
- **Deployment tests** (`QuarkusUnitTest`):
  - each switch, and the master switch
  - build-time conflict detection with a static `robots.txt` and with a JAX-RS `@Path("/robots.txt")`
  - `@SitemapEntry` discovery
  - config fallbacks to Quarkus keys
  - profile-based `indexable`
  - `allowed-hosts`
  - optional caching with and without `quarkus-cache`
  - the Dev UI
- **Integration tests:** a small application with Qute pages, a JAX-RS handler that uses `Seo`, a
  contributor and caching. It runs in JVM mode and native mode. The native build passes
  `-J-XX:TieredStopAtLevel=1,--parallelism=8`.
- **XML check:** the sitemap output is validated against the sitemaps.org schema in tests.

## 15. Documentation

The docs module copies quarkus-microformats, with generated config docs and the same prose rules.
The pages are:
- index (getting started)
- meta tags
- JSON-LD
- sitemap
- robots.txt
- opt-in features
- configuration
- customizing (replaceable beans)
- integrating from another extension
- native
- testing
- releasing

The README follows the quarkus-microformats layout.

## 16. Decisions already made, and by whom

**By the user:**
- Copy the sitemap, robots and meta code out of platform-website, which will use the extension
  later.
- Model the repo on quarkus-microformats.
- Values can be set by the developer (in Java) or in the Qute template.
- The extension serves the sitemap and robots.txt endpoints itself, and the developer can change
  them.
- Caching is the developer's choice.
- Own config keys that fall back to existing Quarkus keys.
- Languages can change at run time and must be easy to change.
- The sitemap and other paths have their own config.
- Always fall back. Never be noisy.
- Do not assume a known proxy.
- Every feature is optional.
- No dependencies between extensions, but plug-and-play integration.

**By the designer (the user can still change these):**
- Maven, Quarkus 3.40.1, release 21.
- A hard dependency on `quarkus-qute`.
- Vert.x routes instead of JAX-RS.
- robots.txt, security.txt and the IndexNow key at fixed paths.
- Writing JSON-LD without Jackson.
- `allowed-hosts`.
- Defaults: indexable only in prod, AI preset `allow`, Cache-Control `public, max-age=3600`.
- Capability-based integration, with no shared SPI module.
- Dev UI in version 1.
- Leaving out charset, viewport and favicon tags.

## 17. Risks and spikes before the plan

1. **The `{#seo}` section helper and request scope.** Confirm that a section helper from an
   extension jar works with Qute build-time validation. Confirm that it can read the
   request-scoped `Seo` bean when rendering from JAX-RS, from Reactive Routes and from Qute Web.
2. **Host-root files under a root path.** Confirm what `RouteBuildItem` can register outside
   `quarkus.http.root-path`. The answer decides which branch of §8.1 applies for all four
   host-root files.
3. **Conflict detection.** Confirm that the deployment module can see application static
   resources and JAX-RS paths in time, for both RESTEasy Reactive and Reactive Routes.
4. **Optional `quarkus-cache`.** Confirm the pattern for using `CacheManager` only when the
   capability is present, in JVM and native mode.
5. **The X-Robots-Tag filter** must run for every response, static files included, without
   buffering bodies.

## 18. Left to the builder

- The exact Java names of builder methods and config groups, within the shapes given here.
- Whether `SitemapContributor` also gets a Mutiny variant.
- The tag order inside `{#seo}`.
- Dev UI layout.
- How the ETag is computed.
- Test fixture content.
