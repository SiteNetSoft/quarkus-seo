# Quarkus SEO Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `quarkus-seo` and `quarkus-seo-deployment` with meta tags (`{#seo /}`, the `Seo` bean, X-Robots-Tag), JSON-LD, the sitemap and robots.txt, conflict detection, optional caching, a native-tested sample application, docs and CI.

**Architecture:** The runtime jar holds a JDK-only `.schema` package for JSON-LD, a public API package, and internals in `.runtime`. The runtime jar is not a bean archive. The deployment module registers each bean, route, filter and Qute helper only when its feature is on and the application does not own the path. Routes are plain Vert.x routes from `RouteBuildItem`, so paths come from build-time config. A resolver merges config defaults, `{#seo}` tag parameters and `Seo` values into one `ResolvedSeo` record. A replaceable renderer turns that record into head tags.

**Tech Stack:** Java 21 bytecode on JDK 25, Maven 3.9, Quarkus 3.40.1 (ArC, Vert.x HTTP, Qute, optional Cache), JUnit 5, AssertJ, REST Assured, Antora docs.

**Spec:** `docs/design/specs/2026-10-29-quarkus-seo-design.md`

**Scope:** This is plan 1 of 2. Plan 2 covers security.txt, the web manifest, IndexNow, llms.txt (spec §9), the Dev UI (§12) and the "opt-in features" docs page. Write plan 2 after this plan is merged, so that it uses the real names from this code.

## Global Constraints

- Quarkus `3.40.1` through `io.quarkus:quarkus-bom`. Only `assertj-core` 3.27.7 and the plugins have their own versions.
- GroupId `org.sitenetsoft`, version `0.1.0-SNAPSHOT`, Apache 2.0 (a LICENSE file only, no file headers). Artifacts: `quarkus-seo-parent`, `quarkus-seo`, `quarkus-seo-deployment`, `quarkus-seo-integration-tests`, `quarkus-seo-docs`.
- `maven.compiler.release=21`. Every Maven command runs with `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`.
- Host build rules: Maven `-T 4` at most. Forked test JVMs run with `-XX:TieredStopAtLevel=1` (root pom `test.argLine`, copied from quarkus-microformats). Native builds always pass `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`. Run one heavy job at a time. Commit before any native build.
- Packages: public API `org.sitenetsoft.quarkus.seo`; JSON-LD `org.sitenetsoft.quarkus.seo.schema`; internals `org.sitenetsoft.quarkus.seo.runtime`; build steps `org.sitenetsoft.quarkus.seo.deployment`.
- `org.sitenetsoft.quarkus.seo.schema` imports only `java.*`. No other quarkus-seo package is imported by it.
- The runtime jar has no `META-INF/beans.xml` and no Jandex index. The deployment module adds each bean and indexed class with `AdditionalBeanBuildItem` or `AdditionalIndexedClassesBuildItem`, so a switched-off feature leaves nothing behind.
- Feature name `seo`. Capability `org.sitenetsoft.seo`. Config prefix `quarkus.seo`.
- Runtime dependencies: `quarkus-arc`, `quarkus-vertx-http`, `quarkus-qute`. `quarkus-cache` is `<optional>true</optional>`. No Jackson.
- Never fail startup because of missing SEO config. Never log above DEBUG in prod, except the one INFO message of Task 13 and errors thrown by application code. Content checks are plan 2 (Dev UI).
- Every replaceable service is an interface with a `@DefaultBean` implementation: `SeoLocales`, `SeoHeadRenderer`, `SitemapWriter`, `RobotsTxtProvider`, `BaseUrlResolver`.
- Prose rules for every doc page, README section, Javadoc comment and commit message (quarkusio/quarkus PR 57821): one idea per sentence of about twenty words, active voice and present tense, condition before action, no clause-joining semicolons or dashes, no essential fact in parentheses, one term per concept, plain words. The full list is in `~/Projects/SiteNetSoft/quarkus-microformats/.superpowers/sdd/2026-10-26-quarkus-microformats-parser/global-constraints.md`.
- Javadoc rules (PR 57819): every new type, constructor, public or package-private method, record (one `@param` per component), enum and enum constant in `main` code gets a Javadoc block. One imperative sentence, then `@param` and `@return`. No Javadoc on test classes. No `@author`, no wildcard imports, no inline fully-qualified names, few lambdas and streams in runtime code, `Optional` for API returns that may be absent.
- Extension descriptions start with an unconjugated verb and do not mention Quarkus, extension or the artifact name.
- Commit messages: one imperative sentence, capitalised, no prefix tags, no trailers, nothing about AI or tooling (`~/Projects/SiteNetSoft/ai-skills/dev-practices/git/git-authorship.md`).

## Review Focus

- A title or description that holds `%`, `{site-name}`, quotes or `<script>` must render escaped and must not throw. The title template uses `String.replace`, never `String.format`. Test in Task 6.
- A request with `Host: evil.example` must not put `evil.example` into canonical, `og:url`, hreflang or sitemap URLs when `allowed-hosts` or `site-url` is set. A `Host` value with `/`, `<` or spaces must be refused in favour of the fallback. Test in Task 4.
- `{#seo /}` rendered with no active request, for example from a scheduled job or a mail template, must render the config defaults with no canonical and no exception. Test in Task 7.
- `quarkus.locales` always has a value in Quarkus (the build machine locale). With neither `quarkus.seo.locales` nor an explicit `quarkus.locales`, the locale list must be `en-US`, not the build machine locale. Test in Task 4.
- A cached sitemap or robots.txt must not be served to a different host. The cache key holds the base URL. Test in Task 14.

---

### Task 1: Maven reactor, descriptor and capability

**Files:**
- Create: `pom.xml`, `runtime/pom.xml`, `deployment/pom.xml`, `.gitignore`, `LICENSE`
- Create: `runtime/src/main/resources/META-INF/quarkus-extension.yaml`
- Create: `deployment/src/main/java/org/sitenetsoft/quarkus/seo/deployment/SeoProcessor.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/DescriptorTest.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/SeoStartupTest.java`

**Interfaces:**
- Produces: root modules `runtime`, `deployment`. `SeoProcessor.FEATURE = "seo"`. Later tasks add build steps to `SeoProcessor`.

- [ ] **Step 1: Commit the spec and this plan.** The repository has no commits yet. Run `git add docs && git commit -m "Add the quarkus-seo design and core plan"`.

- [ ] **Step 2: Write the root pom.** Copy `~/Projects/SiteNetSoft/quarkus-microformats/pom.xml`. Change artifactId to `quarkus-seo-parent`, name to `SEO - Parent`, description to `Add meta tags, sitemaps and robots.txt to web pages`, url and scm to `SiteNetSoft/quarkus-seo`, and modules to `runtime` and `deployment`. Keep the properties, `dependencyManagement`, `pluginManagement`, the `docs` profile and the `release` profile as they are.

- [ ] **Step 3: Write `runtime/pom.xml`.** Copy `quarkus-microformats/parser/runtime/pom.xml`. Set parent `quarkus-seo-parent`, artifactId `quarkus-seo`, name `SEO - Runtime`, description `Add meta tags, sitemaps and robots.txt to web pages`. Dependencies: `quarkus-arc`, `quarkus-vertx-http`, `quarkus-qute`, `quarkus-cache` with `<optional>true</optional>`, and test-scoped `junit-jupiter` and `assertj-core`. In the `quarkus-extension-maven-plugin` configuration, add:

```xml
<capabilities>
  <provides>org.sitenetsoft.seo</provides>
</capabilities>
```

- [ ] **Step 4: Write `deployment/pom.xml`.** Copy `quarkus-microformats/parser/deployment/pom.xml`. Set artifactId `quarkus-seo-deployment`, name `SEO - Deployment`, description `Register meta tags, sitemaps and robots.txt at build time`. Compile dependencies: `quarkus-seo`, `quarkus-arc-deployment`, `quarkus-vertx-http-deployment`, `quarkus-qute-deployment`, `quarkus-devui-deployment-spi`. Test dependencies: `quarkus-junit-internal`, `assertj-core`, `rest-assured`, `quarkus-rest-deployment`, `quarkus-rest-qute-deployment`, `quarkus-reactive-routes-deployment`. Do not add `quarkus-cache-deployment`, so the deployment tests run without a cache.

- [ ] **Step 5: Write the descriptor** `runtime/src/main/resources/META-INF/quarkus-extension.yaml`: name `SEO`, description `Add meta tags, sitemaps and robots.txt to web pages`, artifactId `quarkus-seo`, keywords `seo`, `sitemap`, `robots`, `meta`, `open-graph`, `json-ld`, categories `web`, status `preview`, config `quarkus.seo.`, guide `https://sitenetsoft.github.io/quarkus-seo/`.

- [ ] **Step 6: Write `.gitignore`** with `target/`, `.idea/`, `*.iml`, `.vscode/`, `docs/modules/ROOT/pages/includes/quarkus-seo*.adoc`. Copy `LICENSE` from quarkus-microformats.

- [ ] **Step 7: Write the failing tests.**

```java
// DescriptorTest
@Test
void descriptorProvidesTheCapability() throws IOException {
    Properties p = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/META-INF/quarkus-extension.properties")) {
        p.load(in);
    }
    assertThat(p.getProperty("provides-capabilities")).isEqualTo("org.sitenetsoft.seo");
}

// SeoStartupTest
@RegisterExtension
static final QuarkusUnitTest TEST = new QuarkusUnitTest()
        .withEmptyApplication()
        .setLogRecordPredicate(r -> r.getMessage() != null && r.getMessage().contains("Installed features"))
        .assertLogRecords(records -> assertThat(records)
                .anyMatch(r -> String.valueOf(Arrays.toString(r.getParameters())).contains("seo")));

@Test
void startsWithNoSeoConfigAndInstallsTheFeature() {
    // The assertion is in assertLogRecords. Startup must not fail with no quarkus.seo keys.
}
```

- [ ] **Step 8: Run them.** `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -B -ntp -T 4 verify`. Expected: `SeoStartupTest` fails, because no build step installs the `seo` feature yet.

- [ ] **Step 9: Write `SeoProcessor`** with `@BuildStep FeatureBuildItem feature()` that returns `new FeatureBuildItem(FEATURE)`.

- [ ] **Step 10: Run the build again.** Expected: BUILD SUCCESS, both tests pass.

- [ ] **Step 11: Commit.** `git commit -m "Set up the Maven reactor and the extension descriptor"`

---

### Task 2: Check the five risks before building on them

The spec (§17) asks for five spikes. Each one is a throwaway `QuarkusUnitTest` in `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/spike/`. The expected outcome is listed with the fallback. Later tasks assume the expected outcome. If a spike gives the fallback, update the named task in this plan before you start it.

**Files:**
- Create (then delete): `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/spike/*`
- Create: `docs/design/spikes/2026-11-02-quarkus-seo-spikes.md`

- [ ] **Step 1: Spike 1, the Qute section helper.** Write a `SectionHelperFactory` named `spike` annotated `@EngineConfiguration`, add its class with `AdditionalIndexedClassesBuildItem` from a test build step, and render `{#spike title="x" /}` from a JAX-RS method, a Reactive Route and a Qute Web template. Inside `resolve()`, look up a `@RequestScoped` bean through `Arc.container()` when `requestContext().isActive()`. Expected: the build validates the template, and all three render the bean value. Fallback: ship `templates/tags/seo.html` in the runtime jar as a user tag that calls `{seo:head(...)}` from a `@TemplateExtension(namespace = "seo")` class. Changes Task 7.

- [ ] **Step 2: Spike 2, host-root routes.** With `quarkus.http.root-path=/app`, register `/robots.txt` through `NonApplicationRootPathBuildItem.routeBuilder().routeFunction("/robots.txt", ...)` with an absolute path, or through `RouteBuildItem` with `RouteType.ABSOLUTE_ROUTE`. Request `/robots.txt` and `/app/robots.txt`. Expected: `/robots.txt` answers 200. Fallback: branch 2 of spec §8.1 (serve nothing, log at DEBUG). Changes Task 11.

- [ ] **Step 3: Spike 3, conflict detection.** In a build step, look for `META-INF/resources/robots.txt` in `ApplicationArchivesBuildItem.getRootArchive()` and in each dependency archive. Read `@Path` (`jakarta.ws.rs.Path`) on classes and methods, and `@Route` (`io.quarkus.vertx.web.Route`) `path` values, from `CombinedIndexBuildItem`. Join class and method `@Path` values. Expected: all three are visible before routes are registered, with no compile dependency on REST or Reactive Routes. Fallback: if a source is not visible, drop it from Task 12 and list it as a known limit in the docs. Changes Task 12.

- [ ] **Step 4: Spike 4, optional cache.** In a scratch application with `quarkus-cache`, register a bean that injects `CacheManager` only when `Capabilities.isPresent(Capability.CACHE)`. Without `quarkus-cache`, the bean class stays in the jar but is never registered. Check JVM mode only. Task 14 proves native mode with `quarkus-cache` present. Expected: the application starts both with and without `quarkus-cache`, with no missing-class error. Fallback: move the cache-backed class into a separate optional source set loaded by name. Changes Task 13.

- [ ] **Step 5: Spike 5, the X-Robots-Tag filter.** Register a `FilterBuildItem` whose handler calls `routingContext.addHeadersEndHandler(...)` and then `next()`. Request a static file from `META-INF/resources`, a JAX-RS method and a Reactive Route. Expected: the header appears on all three, and a 1 MB static file is streamed, not buffered. Fallback: none expected. If static files are missed, record it and add it to the docs as a known limit. Changes Task 8.

- [ ] **Step 6: Record the results** in `docs/design/spikes/2026-11-02-quarkus-seo-spikes.md`: one section per spike with the outcome, the exact API that worked and any change to this plan. Edit this plan where a fallback applies.

- [ ] **Step 7: Delete the spike code and commit.** `git commit -m "Record the results of the five design spikes"`

---

### Task 3: JSON-LD writer and schema.org builders

**Files:**
- Create in `runtime/src/main/java/org/sitenetsoft/quarkus/seo/schema/`: `JsonWriter.java` (package-private), `SchemaNode.java`, `Organization.java`, `WebSite.java`, `BreadcrumbList.java`, `Article.java`, `JsonLd.java`, `package-info.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/schema/JsonLdTest.java`, `SchemaPackageIsolationTest.java`

**Interfaces:**
- Produces:
  - `public class SchemaNode`: `static SchemaNode of(String type)`, `SchemaNode id(String id)`, `SchemaNode set(String property, Object value)`, `SchemaNode add(String property, Object value)` (appends to a list), `String type()`, `Optional<String> id()`. A value is a `String`, `Number`, `Boolean`, `SchemaNode`, `Instant`, `OffsetDateTime`, `LocalDate`, `URI` or a `List` of these. A null value removes the property.
  - `Organization extends SchemaNode`: `static Organization create()`, `name(String)`, `url(String)`, `logo(String)`, `sameAs(List<String>)`.
  - `WebSite extends SchemaNode`: `static WebSite create()`, `name(String)`, `url(String)`, `publisher(SchemaNode)`.
  - `BreadcrumbList extends SchemaNode`: `static BreadcrumbList create()`, `item(String name, String url)`. Positions start at 1 in call order.
  - `Article extends SchemaNode`: `static Article create()`, `headline(String)`, `description(String)`, `image(String)`, `datePublished(Instant)`, `dateModified(Instant)`, `author(String name)` (writes a `Person` node), `author(SchemaNode)`.
  - `public final class JsonLd`: `static String graph(List<SchemaNode> nodes, List<String> rawJson)`. It returns `{"@context":"https://schema.org","@graph":[...]}`. Raw JSON is appended to the graph verbatim after `<` is replaced with `\u003c`. Returns an empty string when both lists are empty.
  - Each subclass overrides `id`, `set` and `add` with its own return type, so typed and generic calls chain in any order.

- [ ] **Step 1: Write the failing tests.**

```java
@Test
void writesGraphWithContextAndNodes() {
    String json = JsonLd.graph(List.of(
            Organization.create().id("https://ex.com/#organization").name("Ex").url("https://ex.com/")),
            List.of());
    assertThat(json).isEqualTo("{\"@context\":\"https://schema.org\",\"@graph\":[{\"@type\":\"Organization\","
            + "\"@id\":\"https://ex.com/#organization\",\"name\":\"Ex\",\"url\":\"https://ex.com/\"}]}");
}

@Test
void escapesLessThanSoContentCannotCloseTheScript() {
    String json = JsonLd.graph(List.of(SchemaNode.of("Thing").set("name", "</script><b>")), List.of());
    assertThat(json).doesNotContain("<").contains("\\u003c/script>\\u003cb>");
}

@Test
void escapesQuotesBackslashesControlAndLineSeparators() {
    String json = JsonLd.graph(List.of(SchemaNode.of("Thing").set("name", "a\"b\\c\nd\u2028e")), List.of());
    assertThat(json).contains("\"a\\\"b\\\\c\\nd\\u2028e\"");
}

@Test
void embedsRawJsonVerbatimExceptLessThan() {
    String json = JsonLd.graph(List.of(), List.of("{\"@type\":\"Product\",\"name\":\"</x>\"}"));
    assertThat(json).endsWith("[{\"@type\":\"Product\",\"name\":\"\\u003c/x>\"}]}");
}

@Test
void numbersBooleansDatesAndLists() {
    String json = JsonLd.graph(List.of(SchemaNode.of("Thing")
            .set("n", 3).set("b", true)
            .set("d", Instant.parse("2026-01-02T03:04:05Z"))
            .add("k", "x").add("k", "y")), List.of());
    assertThat(json).contains("\"n\":3,\"b\":true,\"d\":\"2026-01-02T03:04:05Z\",\"k\":[\"x\",\"y\"]");
}

@Test
void breadcrumbPositionsStartAtOne() {
    String json = JsonLd.graph(List.of(BreadcrumbList.create().item("Home", "https://ex.com/").item("Docs", "https://ex.com/docs")), List.of());
    assertThat(json).contains("\"position\":1,\"name\":\"Home\"").contains("\"position\":2,\"name\":\"Docs\"");
}

@Test
void emptyInputGivesEmptyString() {
    assertThat(JsonLd.graph(List.of(), List.of())).isEmpty();
}

// SchemaPackageIsolationTest: reads every .java file under
// runtime/src/main/java/org/sitenetsoft/quarkus/seo/schema and asserts that each
// "import " line starts with "import java." or "import static java.".
@Test
void schemaPackageImportsOnlyTheJdk() throws IOException { ... }
```

- [ ] **Step 2: Run them.** `mvn -B -ntp -pl runtime test -Dtest='JsonLdTest,SchemaPackageIsolationTest'`. Expected: compile failure, the classes do not exist.

- [ ] **Step 3: Implement the classes.** Properties keep insertion order (`LinkedHashMap`). `@type` comes first, then `@id`, then properties. `ListItem` nodes in `BreadcrumbList` have `position`, `name`, `item`, in that order. String escaping writes `"`, `\`, `\b`, `\f`, `\n`, `\r`, `\t`, other characters below U+0020 as `\uXXXX`, U+2028, U+2029 and `<` as `\uXXXX`.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Write schema.org JSON-LD with typed builders and no dependencies"`

---

### Task 4: Config, locales and the base URL

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SeoConfig.java` (`RUN_TIME`), `SeoBuildConfig.java` (`BUILD_AND_RUN_TIME_FIXED`), `SeoSettings.java`, `DefaultSeoLocales.java`, `DefaultBaseUrlResolver.java`, `Urls.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/SeoLocales.java`, `BaseUrlResolver.java`
- Modify: `deployment/.../SeoProcessor.java` (register the beans)
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/UrlsTest.java`, `SeoSettingsTest.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/ConfigFallbackTest.java`, `LocaleFallbackTest.java`, `IndexableProfileTest.java`, `AllowedHostsTest.java`

**Interfaces:**
- Produces:
  - `SeoBuildConfig` keys (all `@WithDefault`): `enabled` true, `meta.enabled` true, `json-ld.enabled` true, `sitemap.enabled` true, `sitemap.path` `/sitemap.xml`, `sitemap.page-path` `/sitemap-{n}.xml`, `robots.enabled` true, `robots.serve-under-root-path` false.
  - `SeoConfig` keys: `site-name`, `site-url`, `locales` (`Optional<List<String>>`), `default-locale`, `indexable` (`Optional<Boolean>`), `allowed-hosts` (`Optional<List<String>>`), `title.template`, `title.default`, `description`, `image`, `image-alt`, `twitter.site`, `twitter.creator`, `verification.google`, `verification.bing`, `verification.yandex`, `theme-color`, `canonical.keep-query-params`, `organization.enabled` (default true), `organization.name`, `organization.url`, `organization.logo`, `organization.same-as`, `website.enabled` (default true), `sitemap.urls`, `sitemap.max-urls` (default 50000), `sitemap.cache-control` (default `public, max-age=3600`), `sitemap.cache.enabled` (default false), `robots.groups` (`Map<String, RobotsGroupConfig>` with `user-agents`, `allow`, `disallow`, `crawl-delay`), `robots.ai` (enum `AiPreset { ALLOW, BLOCK_TRAINING, BLOCK_ALL }`, default `allow`), `robots.ai-agents.training`, `robots.ai-agents.search`, `robots.cache.enabled` (default false). Every value without a default is `Optional`.
  - `@Singleton SeoSettings`: `Optional<String> siteName()`, `boolean indexable()`, `String titleTemplate()`, `Optional<String> defaultTitle()`, `String rootPath()` (no trailing slash, `""` for `/`). Static helper `static boolean indexable(Optional<Boolean> configured, List<String> activeProfiles)`.
  - `public interface SeoLocales`: `List<Locale> locales()`, `Locale defaultLocale()`. `DefaultSeoLocales` is `@DefaultBean @Singleton`.
  - `public interface BaseUrlResolver`: `String resolve(RoutingContext context)`. It returns the public base of the application with no trailing slash, for example `https://ex.com` or `https://ex.com/app`. `DefaultBaseUrlResolver` is `@DefaultBean @Singleton`.
  - `final class Urls`: `static String absolute(String base, String pathOrUrl)`, `static String canonicalQuery(String rawQuery, List<String> keep)`, `static boolean isSafeHost(String host)`.

- [ ] **Step 1: Write the failing unit tests.**

```java
// UrlsTest
@Test void keepsAbsoluteHttpUrls() { assertThat(Urls.absolute("https://ex.com", "http://cdn.ex.com/a.png")).isEqualTo("http://cdn.ex.com/a.png"); }
@Test void joinsRelativePathsWithOneSlash() {
    assertThat(Urls.absolute("https://ex.com/app", "/en/about")).isEqualTo("https://ex.com/app/en/about");
    assertThat(Urls.absolute("https://ex.com/app", "en/about")).isEqualTo("https://ex.com/app/en/about");
}
@Test void schemeRelativeTakesTheBaseScheme() { assertThat(Urls.absolute("https://ex.com", "//cdn.ex.com/a")).isEqualTo("https://cdn.ex.com/a"); }
@Test void canonicalQueryKeepsListedParamsInRequestOrder() {
    assertThat(Urls.canonicalQuery("utm_source=x&page=2&lang=fr", List.of("lang", "page"))).isEqualTo("page=2&lang=fr");
    assertThat(Urls.canonicalQuery("utm_source=x", List.of())).isEmpty();
}
@Test void refusesHostsWithPathOrMarkup() {
    assertThat(Urls.isSafeHost("ex.com:8443")).isTrue();
    assertThat(Urls.isSafeHost("[::1]:8080")).isTrue();
    assertThat(Urls.isSafeHost("ex.com/evil")).isFalse();
    assertThat(Urls.isSafeHost("ex.com\"><script>")).isFalse();
    assertThat(Urls.isSafeHost("")).isFalse();
}

// SeoSettingsTest
@Test void indexableOnlyInProdByDefault() {
    assertThat(SeoSettings.indexable(Optional.empty(), List.of("prod"))).isTrue();
    assertThat(SeoSettings.indexable(Optional.empty(), List.of("dev"))).isFalse();
    assertThat(SeoSettings.indexable(Optional.of(true), List.of("test"))).isTrue();
    assertThat(SeoSettings.indexable(Optional.of(false), List.of("prod"))).isFalse();
}
```

- [ ] **Step 2: Write the failing deployment tests.** Each uses `QuarkusUnitTest` with a small `@Path` resource that returns values from the injected beans as text.

  - `ConfigFallbackTest`:
    - `quarkus.application.name=Acme` with no `quarkus.seo.site-name` gives site name `Acme`.
    - `quarkus.seo.site-name=Shop` wins over it.
    - With no locale keys set, `locales()` is `[en-US]` and `defaultLocale()` is `en-US`, whatever the build machine locale is. Put this case in its own class, `LocaleFallbackTest`. Its static initializer sets the system properties `user.language=fr` and `user.country=CA` before the `QuarkusUnitTest` field, because the Quarkus default of `quarkus.locales` comes from them.
    - `quarkus.locales=fr,en` gives `[fr, en]`, default `fr`.
    - `quarkus.seo.default-locale=en` with `quarkus.seo.locales=fr,en` gives default `en`.
  - `IndexableProfileTest`: under the test profile, `indexable()` is false. With `quarkus.seo.indexable=true`, it is true.
  - `AllowedHostsTest`:
    - With `quarkus.seo.allowed-hosts=ex.com,www.ex.com`, a request with header `Host: evil.example` resolves to `http://ex.com`.
    - A request with `Host: www.ex.com` resolves to `http://www.ex.com`.
    - With `quarkus.seo.site-url=https://ex.com/`, any host resolves to `https://ex.com`.
    - With no setting, `Host: evil.example/<x>` is refused by `isSafeHost`, and the resolver falls back to `quarkus.http.host` and the server port.

- [ ] **Step 3: Run them.** Expected: compile failures.

- [ ] **Step 4: Implement.**
  - `DefaultSeoLocales` reads `quarkus.seo.locales`. Next it reads `quarkus.locales` and `quarkus.default-locale`, but only when their `ConfigValue.getSourceOrdinal()` is above the Quarkus default-values ordinal. That way the build machine locale never leaks. The final fallback is `en-US`.
  - `DefaultBaseUrlResolver` follows spec §4.3. It uses `site-url` with the trailing slash removed and no root path appended. Otherwise it takes `context.request().scheme()` and `context.request().authority()` as Quarkus resolved them, then appends `SeoSettings.rootPath()`. An unsafe host or a host outside `allowed-hosts` is replaced by the first allowed host, or else by `quarkus.http.host` and the local port.
  - `SeoSettings.indexable()` calls the static helper with `ConfigUtils.getProfiles()`.
  - In `SeoProcessor`, add a step that returns `AdditionalBeanBuildItem` for `SeoSettings`, `DefaultSeoLocales` and `DefaultBaseUrlResolver`, made unremovable, when `quarkus.seo.enabled` is true.

- [ ] **Step 5: Run the tests.** `mvn -B -ntp -T 4 verify`. Expected: PASS.

- [ ] **Step 6: Commit.** `git commit -m "Resolve the site settings, locales and base URL with Quarkus fallbacks"`

---

### Task 5: The `Seo` bean, its sources and the merge

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/Seo.java`, `SeoSource.java`, `ResolvedSeo.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SeoValues.java`, `SeoResolver.java`, `RequestInfo.java`, `RobotsHeader.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/SeoValuesTest.java`, `SeoResolverTest.java`

**Interfaces:**
- Consumes: `SeoSettings`, `SeoLocales`, `SeoConfig`, `SeoBuildConfig`, `Urls` (Task 4); `SchemaNode`, `Organization`, `WebSite`, `JsonLd` (Task 3).
- Produces:
  - `public class SeoValues` in `.runtime`. Each field has a fluent setter and an accessor of the same name, for example `title(String)` and `title()`. Lists use `alternate(Locale, String)`, `tag(String)`, `jsonLd(SchemaNode)`, `jsonLdRaw(String)` and `meta(ResolvedSeo.Meta)`. Nullable fields: `title`, `absoluteTitle`, `description`, `canonical`, `type`, `image`, `imageWidth`, `imageHeight`, `imageAlt`, `locale`, `published`, `modified`, `author`, `section`, `twitterCard`, `maxSnippet`, `maxImagePreview`, `unavailableAfter`. Booleans as `Boolean`: `noindex`, `nofollow`, `noimageindex`. Lists: `alternates` (`LinkedHashMap<Locale, String>`), `tags`, `jsonLdNodes`, `jsonLdRaw`, `extraMeta` (`List<ResolvedSeo.Meta>`). Method `SeoValues over(SeoValues lower)` returns a new value. In it, each set field of `this` replaces the field of `lower`, and lists hold `lower` first and then `this`. A blank string counts as not set when it is stored.
  - `final class RobotsHeader`: `static final String KEY = "org.sitenetsoft.seo.robots"`, `static void put(RoutingContext context, List<String> directives)`, `static List<String> get(RoutingContext context)`.
  - `public record RequestInfo(String baseUrl, String path, String rawQuery, boolean home)`. `path` is relative to the root path and starts with `/`. `static Optional<RequestInfo> current(BaseUrlResolver resolver, SeoSettings settings)` reads `CurrentVertxRequest` and is empty with no active request.
  - `@Singleton public class SeoResolver`: `ResolvedSeo resolve(SeoValues tag, SeoValues java, Optional<RequestInfo> request)`.
  - `public record ResolvedSeo(...)` with these components, in this order: `String title`, `Optional<String> description`, `List<String> robots`, `Optional<String> canonical`, `List<Link> hreflang`, `String type`, `Optional<String> ogTitle`, `Optional<String> siteName`, `String ogLocale`, `List<String> ogLocaleAlternates`, `Optional<Image> image`, `Optional<Article> article`, `String twitterCard`, `Optional<String> twitterSite`, `Optional<String> twitterCreator`, `List<Meta> verification`, `Optional<String> themeColor`, `List<Meta> extraMeta`, `String jsonLd`. Nested records: `Link(String hreflang, String href)`, `Image(String url, Integer width, Integer height, String alt)`, `Article(Optional<Instant> published, Optional<Instant> modified, Optional<String> author, Optional<String> section, List<String> tags)`, `Meta(String attribute, String key, String content)` where `attribute` is `name` or `property`.
  - `@RequestScoped public class Seo`. Fluent setters that return `Seo`: `title`, `absoluteTitle`, `description`, `canonical`, `type`, `image(String)`, `image(String url, int width, int height)`, `imageAlt`, `locale(Locale)`, `alternate(Locale, String)`, `noindex()`, `nofollow()`, `noimageindex()`, `maxSnippet(int)`, `maxImagePreview(String)`, `unavailableAfter(Instant)`, `published(Instant)`, `modified(Instant)`, `author`, `section`, `tag(String...)`, `twitterCard(String)`, `jsonLd(SchemaNode)`, `jsonLdRaw(String)`, `meta(String name, String content)`, `property(String property, String content)`, `from(SeoSource)`. Other methods: `ResolvedSeo resolve()`, `String head()` (added in Task 7), `SeoValues javaValues()`, `SeoValues tagValues()`, `void tagValues(SeoValues values)` (used by the Qute tag).
  - `public interface SeoSource`: default methods that return `Optional.empty()`: `title`, `description`, `canonical`, `type`, `image`, `imageAlt`, `locale` (`Optional<Locale>`), `published`, `modified` (`Optional<Instant>`), `author`, `section`, `noindex` (`Optional<Boolean>`). `alternates()` returns `Map.of()` by default, and `tags()` returns `List.of()`.

- [ ] **Step 1: Write the failing tests.** `SeoResolverTest` builds the resolver with plain constructors and fake `SeoSettings`, `SeoLocales` and config values. Use a small `TestConfig` helper that implements the config interfaces.

```java
// SeoValuesTest
@Test void higherLayerReplacesOnlyTheFieldsItSets() {
    SeoValues lower = new SeoValues().title("A").image("/a.png");
    SeoValues merged = new SeoValues().title("B").over(lower);
    assertThat(merged.title()).isEqualTo("B");
    assertThat(merged.image()).isEqualTo("/a.png");
}
@Test void listsAddLowerFirst() {
    SeoValues merged = new SeoValues().alternate(Locale.FRENCH, "/fr").over(new SeoValues().alternate(Locale.ENGLISH, "/en"));
    assertThat(merged.alternates()).containsExactly(entry(Locale.ENGLISH, "/en"), entry(Locale.FRENCH, "/fr"));
}
@Test void blankCountsAsNotSet() {
    assertThat(new SeoValues().title("  ").over(new SeoValues().title("A")).title()).isEqualTo("A");
}

// SeoResolverTest (site name "Acme", base https://ex.com, request path /about?utm=1&page=2, keep [page])
@Test void javaWinsOverTagWhichWinsOverConfig() { /* config description "C", tag "T", java "J" -> "J"; no java -> "T" */ }
@Test void titleTemplateAppliesToPageTitleOnly() {
    // page title "About" -> "About | Acme"; no page title -> "Acme" (title.default = site name, no template)
    // absoluteTitle "Exact" -> "Exact"
}
@Test void titleWithPercentAndPlaceholderDoesNotThrow() {
    // title "100% {site-name}" -> "100% {site-name} | Acme"
}
@Test void noSiteNameMeansPlainTemplate() { /* no site name, title "About" -> "About" */ }
@Test void canonicalDropsUnlistedQueryParams() { /* -> "https://ex.com/about?page=2" */ }
@Test void relativeCanonicalAndImageBecomeAbsolute() { /* canonical "/x" -> https://ex.com/x ; image "/i.png" -> https://ex.com/i.png */ }
@Test void hreflangHasSelfAlternatesAndXDefault() {
    // default locale en, page locale fr, alternate en -> /en/about
    // -> [Link("fr", canonical), Link("en", "https://ex.com/en/about"), Link("x-default", "https://ex.com/en/about")]
}
@Test void noAlternatesMeansNoHreflang() { }
@Test void xDefaultIsLeftOutWhenNoLinkHasTheDefaultLocale() { }
@Test void ogLocaleUsesUnderscore() { /* Locale.forLanguageTag("en-US") -> "en_US"; fr -> "fr" */ }
@Test void twitterCardDependsOnImage() { /* image -> summary_large_image ; none -> summary ; twitterCard set -> as set */ }
@Test void notIndexableForcesNoindexNofollow() { /* indexable false -> robots [noindex, nofollow] whatever the page set */ }
@Test void robotsDirectivesInFixedOrder() {
    // noindex, noimageindex, maxSnippet 50, maxImagePreview large, unavailableAfter 2027-01-01T00:00:00Z
    // -> [noindex, noimageindex, max-snippet:50, max-image-preview:large, unavailable_after: 2027-01-01T00:00:00Z]
}
@Test void articleFieldsOnlyForArticleType() { }
@Test void homePageGetsOrganizationAndWebSite() {
    // organization.name "Acme Inc" -> graph has Organization @id https://ex.com/#organization,
    // WebSite @id https://ex.com/#website with publisher {"@id":"https://ex.com/#organization"}
}
@Test void otherPagesGetNoSiteWideNodes() { }
@Test void jsonLdSwitchedOffGivesEmptyString() { }
@Test void noRequestMeansNoCanonicalNoHreflangAndRelativeUrlsLeftOut() {
    // resolve(tag, java, Optional.empty()) -> canonical empty, hreflang empty, relative image left out
}
```

- [ ] **Step 2: Run them.** Expected: compile failures.

- [ ] **Step 3: Implement.** Follow spec §5.2 and §5.3. Settled choices:
  - `og:title` is the page title before the template, or the default title.
  - `og:url` is the canonical URL, so it is not a separate component.
  - Article tags appear only when the resolved type is `article`.
  - The `unavailable_after` value uses `DateTimeFormatter.ISO_INSTANT`.
  - Verification metas are `google-site-verification`, `msvalidate.01` and `yandex-verification`.
  - The default type is `website`.
  - `Seo.from(SeoSource)` copies each present value into the Java layer.
  - Each robots setter on `Seo` also calls `RobotsHeader.put` with the Java directives, for Task 8. With no active request, it skips that call.
  - `Seo.resolve()` calls `SeoResolver.resolve(tagValues, javaValues, RequestInfo.current(...))`.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Merge config, tag and Java values into one resolved page record"`

---

### Task 6: The head renderer

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/SeoHeadRenderer.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/DefaultSeoHeadRenderer.java`, `Html.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/DefaultSeoHeadRendererTest.java`

**Interfaces:**
- Consumes: `ResolvedSeo` (Task 5).
- Produces: `public interface SeoHeadRenderer { String render(ResolvedSeo seo); }`. `DefaultSeoHeadRenderer` is `@DefaultBean @Singleton`. `final class Html { static String escape(String text); }` escapes `&`, `<`, `>`, `"` and `'`.

Tag order, one tag per line, each line ending in `\n`:
1. `<title>`
2. `meta name="description"`
3. `meta name="robots"` (directives joined with `, `)
4. `link rel="canonical"`
5. `link rel="alternate" hreflang=...` in the order of `hreflang()`
6. `og:type`, `og:title`, `og:description`, `og:url`, `og:site_name`, `og:locale`, then each `og:locale:alternate`, then `og:image`, `og:image:width`, `og:image:height`, `og:image:alt`
7. `article:published_time`, `article:modified_time`, `article:author`, `article:section`, then each `article:tag`
8. `twitter:card`, `twitter:site`, `twitter:creator`
9. verification metas
10. `theme-color`
11. extra metas
12. `<script type="application/ld+json">` with `jsonLd()`, written as is (Task 3 escaped it)

- [ ] **Step 1: Write the failing tests.**

```java
@Test void fullPageInFixedOrder() {
    // Build a ResolvedSeo with every component set.
    // Assert the output equals one expected multi-line string, held in the test as a text block.
}
@Test void absentValuesWriteNoTag() {
    String html = renderer.render(minimal("Home"));
    assertThat(html).isEqualTo("<title>Home</title>\n<meta property=\"og:type\" content=\"website\">\n"
            + "<meta property=\"og:title\" content=\"Home\">\n<meta property=\"og:locale\" content=\"en_US\">\n"
            + "<meta name=\"twitter:card\" content=\"summary\">\n");
    assertThat(html).doesNotContain("content=\"\"");
}
@Test void escapesEveryAttributeAndTheTitle() {
    String html = renderer.render(minimal("<script>\"x\" & 'y' 100% {site-name}"));
    assertThat(html).contains("<title>&lt;script&gt;&quot;x&quot; &amp; &#39;y&#39; 100% {site-name}</title>");
}
@Test void emptyJsonLdWritesNoScript() { }
```

- [ ] **Step 2: Run them.** Expected: compile failure.

- [ ] **Step 3: Implement** with a `StringBuilder` and one private `meta(attribute, key, content)` helper that skips a null or blank content.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Render the resolved page values as escaped head tags"`

---

### Task 7: `{#seo /}`, the `seo:` namespace and Java rendering

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SeoSectionHelperFactory.java`, `SeoTemplateExtensions.java`
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/Seo.java` (add `head()`)
- Modify: `deployment/.../SeoProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/SeoTagTest.java`, `SeoTagNoRequestTest.java`, `SeoJavaRenderTest.java`, `UnknownTagParameterTest.java`

This task assumes spike 1 gave the expected result. Otherwise use the fallback recorded in Task 2.

**Interfaces:**
- Consumes: `Seo`, `SeoValues`, `SeoResolver`, `RequestInfo` (Task 5); `SeoHeadRenderer` (Task 6).
- Produces:
  - Tag name `seo`. Parameters: `title`, `absoluteTitle`, `description`, `canonical`, `type`, `image`, `imageWidth`, `imageHeight`, `imageAlt`, `locale` (a BCP 47 string), `noindex`, `nofollow`, `noimageindex`, `twitterCard`, `author`, `section`. An unknown parameter name fails the build with a message that lists the known names.
  - `seo:` namespace methods: `title`, `description`, `canonical`, `image`, `locale`, `siteName`. Each returns the resolved `String`, or an empty string.
  - `Seo.head()` returns `renderer.render(resolve())`.
  - Build step: when `meta.enabled` and `enabled` are true, register `Seo`, `SeoResolver`, `DefaultSeoHeadRenderer` and both Qute classes with `AdditionalBeanBuildItem`. Qute reads `@EngineConfiguration` and `@TemplateExtension` classes from the bean archive index only. Spike 1 of Task 2 shows that `AdditionalIndexedClassesBuildItem` alone leaves the tag and the namespace unregistered.

- [ ] **Step 1: Write the failing tests.**
  - `SeoTagTest` uses a JAX-RS resource with `@Inject Seo seo` that returns a `TemplateInstance`. The template is `<head>{#seo title="About" description="Tag" /}</head><h1>{seo:title}</h1>`.
    - `tagValuesRender`: GET `/about` contains `<title>About</title>` and `<h1>About</h1>`.
    - `javaWinsOverTag`: the resource sets `seo.description("Java")`. The output contains `content="Java"` and not `content="Tag"`.
    - `messageBundleKeysWork`: `{#seo title=msg:about_title /}` with a `@MessageBundle` interface renders the bundle value.
    - `canonicalUsesRequestPath`: GET `/about?utm=1` contains `<link rel="canonical" href="http://localhost:` and `/about">`.
  - `SeoTagNoRequestTest`: a `@Startup` bean renders the same template at startup, with no request active. Expected: no exception, the output contains `<title>About</title>`, and it holds no `canonical`.
  - `SeoJavaRenderTest`: a Reactive Route `@Route(path = "/plain")` sets `seo.title("Plain")` and returns `seo.head()`. Expected: the body starts with `<title>Plain</title>`.
  - `UnknownTagParameterTest`: `{#seo titel="x" /}` fails the build. Use `.assertException(t -> assertThat(t).hasMessageContaining("titel").hasMessageContaining("title"))`.

- [ ] **Step 2: Run them.** `mvn -B -ntp -pl deployment -am verify -Dtest='SeoTag*,SeoJavaRenderTest,UnknownTagParameterTest' -Dsurefire.failIfNoSpecifiedTests=false`. Expected: FAIL.

- [ ] **Step 3: Implement the factory.** In `initializeBlock`, check the parameter names. Qute passes every named parameter to `BlockInfo.getParameters()` under its own name, and each positional parameter under its index, such as `0`. Throw `block.error(...)` with `.origin(block.getOrigin())` and pass the names through `.argument(...)`. A literal brace in the message breaks the message template. In `resolve`, evaluate the parameters, build a tag `SeoValues`, store it on the request `Seo` through `Seo.tagValues(...)`, and render through `SeoHeadRenderer`. With no active request context, use a fresh `Seo` that is not stored. The `seo:` methods resolve through the same request `Seo`, so they see the tag values after the head has rendered.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Add the seo section tag, the seo namespace and Java head rendering"`

---

### Task 8: X-Robots-Tag filter

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SeoRecorder.java`
- Modify: `deployment/.../SeoProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/RobotsHeaderTest.java`, `RobotsHeaderIndexableTest.java`

**Interfaces:**
- Consumes: `RobotsHeader.KEY` written by `Seo` (Task 5); `SeoSettings.indexable()`.
- Produces: `@Recorder SeoRecorder` with `Handler<RoutingContext> robotsHeaderFilter()`. The filter is registered with `FilterBuildItem` at priority 100 when `meta.enabled` is true.

- [ ] **Step 1: Write the failing tests.**
  - `RobotsHeaderTest` runs under the test profile, so `indexable` is false.
    - A static file `META-INF/resources/file.pdf` has header `X-Robots-Tag: noindex, nofollow`.
    - A JAX-RS method has the same header.
  - `RobotsHeaderIndexableTest` sets `quarkus.seo.indexable=true`.
    - A plain JAX-RS method has no `X-Robots-Tag`.
    - A method that calls `seo.noindex().noimageindex()` has `X-Robots-Tag: noindex, noimageindex`.
    - A template that sets `noindex` only on `{#seo}` has the meta tag and no header.

- [ ] **Step 2: Run them.** Expected: FAIL.

- [ ] **Step 3: Implement.** In the handler, call `addHeadersEndHandler` and then `next()`. The headers-end callback writes the header when the site is not indexable, or when `RobotsHeader.KEY` holds directives. It does not overwrite an `X-Robots-Tag` that the application set.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Send X-Robots-Tag for non-indexable sites and Java-set directives"`

---

### Task 9: Sitemap model and writer

**Files:**
- Create in `runtime/src/main/java/org/sitenetsoft/quarkus/seo/`: `SitemapUrl.java`, `ChangeFrequency.java`, `SitemapContributor.java`, `SitemapEntry.java`, `SitemapWriter.java`
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/DefaultSitemapWriter.java`
- Create: `runtime/src/test/resources/xsd/sitemap.xsd`, `siteindex.xsd`. Download each once from `https://www.sitemaps.org/schemas/sitemap/0.9/` and commit it.
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/DefaultSitemapWriterTest.java`

**Interfaces:**
- Produces:
  - `public final class SitemapUrl`: `static SitemapUrl of(String loc)`, then `lastmod(Instant)`, `changefreq(ChangeFrequency)`, `priority(double)`, `alternate(Locale, String)`, `image(String)` (repeatable), `title(String)`. Accessors: `loc()`, `Optional<Instant> lastmod()`, `Optional<ChangeFrequency> changefreq()`, `OptionalDouble priority()`, `Map<Locale, String> alternates()`, `List<String> images()`, `Optional<String> title()`.
  - `public enum ChangeFrequency { ALWAYS, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, NEVER }`. Each value is written in lower case.
  - `public interface SitemapContributor { Stream<SitemapUrl> urls(); }`
  - `@Retention(RUNTIME) @Target(METHOD) public @interface SitemapEntry { String changefreq() default ""; double priority() default -1; }`
  - `public interface SitemapWriter`: `String urlSet(List<SitemapUrl> urls, String baseUrl, Locale defaultLocale)` and `String index(List<String> pageUrls)`. `DefaultSitemapWriter` is `@DefaultBean @Singleton`.

- [ ] **Step 1: Write the failing tests.**

```java
@Test void urlSetIsValidAgainstTheSchema() { /* validate output with javax.xml.validation and xsd/sitemap.xsd */ }
@Test void indexIsValidAgainstTheSchema() { }
@Test void locIsAbsoluteEscapedAndAscii() {
    String xml = writer.urlSet(List.of(SitemapUrl.of("/café?a=1&b=2")), "https://ex.com", Locale.ENGLISH);
    assertThat(xml).contains("<loc>https://ex.com/caf%C3%A9?a=1&amp;b=2</loc>");
}
@Test void optionalFieldsLeftOutUnlessSet() {
    String xml = writer.urlSet(List.of(SitemapUrl.of("/a")), "https://ex.com", Locale.ENGLISH);
    assertThat(xml).doesNotContain("<lastmod>", "<changefreq>", "<priority>");
}
@Test void lastmodChangefreqPriority() {
    // lastmod 2026-01-02T03:04:05Z, WEEKLY, 0.8
    // -> <lastmod>2026-01-02T03:04:05Z</lastmod><changefreq>weekly</changefreq><priority>0.8</priority>
}
@Test void alternatesWriteSelfOthersAndXDefault() {
    // loc /fr/a, alternates {fr: /fr/a, en: /en/a}, default locale en
    // -> xhtml:link hreflang fr, en and x-default (-> https://ex.com/en/a), namespace xmlns:xhtml declared
}
@Test void imagesUseTheImageNamespace() { /* <image:image><image:loc>https://ex.com/i.png</image:loc></image:image> */ }
```

- [ ] **Step 2: Run them.** Expected: compile failure.

- [ ] **Step 3: Implement** with a `StringBuilder`. Declare the namespaces `http://www.sitemaps.org/schemas/sitemap/0.9`, `http://www.w3.org/1999/xhtml` (prefix `xhtml`) and `http://www.google.com/schemas/sitemap-image/1.1` (prefix `image`) on `urlset`. Make URLs ASCII with `new URI(...).toASCIIString()`. If a URL cannot be parsed, percent-encode its non-ASCII bytes. Then XML-escape the result. Never write the current time as `lastmod`. The alternates map lists every language version, the URL itself included, as Google requires. The writer adds `x-default` for the entry in the default locale.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Write sitemap URL sets and indexes that pass the sitemaps.org schema"`

---

### Task 10: Sitemap sources, routes and headers

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SitemapService.java`, `SitemapEntries.java`, `HttpOutputs.java` (ETag and Cache-Control)
- Modify: `SeoRecorder.java` (add the sitemap handlers), `deployment/.../SeoProcessor.java`
- Create: `deployment/src/main/java/org/sitenetsoft/quarkus/seo/deployment/SitemapEntryScanner.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/SitemapEndpointTest.java`, `SitemapSplitTest.java`, `SitemapEntryValidationTest.java`, `SitemapPathTest.java`, `SitemapContributorFailureTest.java`

**Interfaces:**
- Consumes: `SitemapWriter`, `SitemapUrl`, `SitemapContributor`, `SitemapEntry` (Task 9); `BaseUrlResolver`, `SeoLocales` (Task 4).
- Produces:
  - `@Singleton SitemapService`: `Optional<String> page(String baseUrl, int page)`. Page `0` is the whole sitemap: a `urlset` when the URL count is at most `max-urls`, otherwise the index. Page `n ≥ 1` is that slice. An empty `Optional` means 404. It runs on a worker thread.
  - `SitemapEntries`: the list of paths, changefreq and priority found at build time, set by the recorder.
  - `SitemapEntryScanner` (package-private): `static List<SitemapEntryBuildItem> scan(IndexView index)`. It reads `@SitemapEntry` on methods that also have `@GET` and a `@Path` with no `{`. It joins the class `@Path` and the method `@Path`. It fails the build for an unknown `changefreq`, for a priority outside 0..1 other than the unset -1, and for a path with a template variable.
  - `final class HttpOutputs`: `static void send(RoutingContext context, String body, String contentType, String cacheControl)`. It sets an ETag (the first 16 hex characters of the SHA-256 of the body, in quotes) and answers `304` when `If-None-Match` matches.

- [ ] **Step 1: Write the failing tests.**
  - `SitemapEndpointTest` uses config `quarkus.seo.sitemap.urls=/,/about`, a resource with `@GET @Path("/products") @SitemapEntry(changefreq = "weekly")`, and a contributor bean that returns `/blog/1` with lastmod. It checks that:
    - GET `/sitemap.xml` is 200 with `Content-Type: application/xml; charset=utf-8`.
    - The body contains all four `<loc>` values as absolute URLs.
    - The `Cache-Control` header is `public, max-age=3600`.
    - An `ETag` is present, and a second request with `If-None-Match` gets 304.
    - The contributor stream was closed. The contributor sets a flag in `onClose`.
  - `SitemapSplitTest` sets `quarkus.seo.sitemap.max-urls=2` with five URLs.
    - `/sitemap.xml` is a `sitemapindex` with three `<loc>` entries ending `/sitemap-1.xml` to `/sitemap-3.xml`.
    - `/sitemap-3.xml` has one URL.
    - `/sitemap-4.xml` is 404.
  - `SitemapEntryValidationTest`: `@SitemapEntry(changefreq = "sometimes")` fails the build. So does a path `/items/{id}`.
  - `SitemapPathTest`: with `quarkus.seo.sitemap.path=/maps/site.xml` and `quarkus.http.root-path=/app`, the sitemap answers at `/app/maps/site.xml` and its `loc` values start with `http://localhost:<port>/app/`.
  - `SitemapContributorFailureTest`: a contributor that throws during the stream gives a 500 with an empty body. The error is logged once at ERROR with the contributor class name. The stream is closed.

- [ ] **Step 2: Run them.** Expected: FAIL.

- [ ] **Step 3: Implement.**
  - The deployment registers the routes with `RouteBuildItem` (application routes, under the root path) only when `sitemap.enabled` is true.
  - The page path becomes a regex route. `{n}` is replaced by `([1-9][0-9]*)`, and the other characters are quoted.
  - Each handler runs as a blocking handler.
  - The service drops URLs with a null or blank `loc`. It closes every contributor stream in a `try-with-resources`.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Serve the sitemap from config, annotated endpoints and contributor beans"`

---

### Task 11: robots.txt

**Files:**
- Create in `runtime/src/main/java/org/sitenetsoft/quarkus/seo/`: `RobotsTxtProvider.java`, `RobotsRules.java`, `RobotsGroup.java`, `RobotsCustomizer.java`
- Create in `.runtime`: `DefaultRobotsTxtProvider.java`, `AiCrawlers.java`
- Modify: `SeoRecorder.java`, `deployment/.../SeoProcessor.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/DefaultRobotsTxtProviderTest.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/RobotsEndpointTest.java`, `RobotsRootPathTest.java`

This task assumes spike 2 gave the expected result.

**Interfaces:**
- Consumes: `SeoConfig.robots()`, `SeoSettings.indexable()`, `BaseUrlResolver`, `SeoBuildConfig.sitemap()`, `HttpOutputs` (Task 10).
- Produces:
  - `public final class RobotsGroup`: `List<String> userAgents()`, `List<String> allow()`, `List<String> disallow()`, `OptionalInt crawlDelay()`. It is mutable, with a public constructor `RobotsGroup(List<String> userAgents)` and the setters `allow(String...)`, `disallow(String...)` and `crawlDelay(int)`.
  - `public final class RobotsRules`: `List<RobotsGroup> groups()` (mutable), `List<String> sitemaps()` (mutable, absolute URLs).
  - `public interface RobotsCustomizer { void customize(RobotsRules rules); }`
  - `public interface RobotsTxtProvider { String robotsTxt(String baseUrl); }`. `DefaultRobotsTxtProvider` is `@DefaultBean @Singleton`.
  - `final class AiCrawlers`: `static final List<String> TRAINING` = `GPTBot`, `ClaudeBot`, `Google-Extended`, `Applebot-Extended`, `CCBot`, `Bytespider`, `Meta-ExternalAgent`, `cohere-ai`, `Amazonbot`. `static final List<String> SEARCH` = `OAI-SearchBot`, `ChatGPT-User`, `Claude-SearchBot`, `Claude-User`, `PerplexityBot`, `Perplexity-User`.

- [ ] **Step 1: Write the failing unit tests.**

```java
@Test void noGroupsAllowsAll() { assertThat(provider(indexable).robotsTxt(BASE)).isEqualTo("User-agent: *\nAllow: /\n\nSitemap: https://ex.com/sitemap.xml\n"); }
@Test void groupsSortedByNameWithAllFields() {
    // groups b(user-agents=Bingbot, disallow=/x) and a(user-agents=*, disallow=/account,/admin, allow=/, crawl-delay=5)
    // -> "User-agent: *\nAllow: /\nDisallow: /account\nDisallow: /admin\nCrawl-delay: 5\n\nUser-agent: Bingbot\nDisallow: /x\n\nSitemap: ...\n"
}
@Test void blockTrainingAddsOneGroupWithTrainingAgents() { /* User-agent lines GPTBot..Amazonbot then Disallow: / */ }
@Test void blockAllAddsTrainingAndSearchAgents() { }
@Test void configuredAgentListsReplaceTheBuiltIns() { }
@Test void notIndexableDisallowsEverythingAndHasNoSitemap() { assertThat(provider(notIndexable).robotsTxt(BASE)).isEqualTo("User-agent: *\nDisallow: /\n"); }
@Test void noSitemapLineWhenSitemapDisabled() { }
@Test void customizerRunsAfterConfig() { }
```

- [ ] **Step 2: Write the failing deployment tests.**
  - `RobotsEndpointTest`: GET `/robots.txt` is 200 with `text/plain; charset=utf-8`, an ETag and `Cache-Control: public, max-age=3600`.
  - `RobotsRootPathTest`: with `quarkus.http.root-path=/app`, `/robots.txt` is 200, its `Sitemap:` line ends with `/app/sitemap.xml`, and `/app/robots.txt` is 404. A second class sets `quarkus.seo.robots.serve-under-root-path=true`. Then `/app/robots.txt` is 200 and `/robots.txt` is 404.

- [ ] **Step 3: Run them.** Expected: FAIL.

- [ ] **Step 4: Implement.** The Cache-Control value reuses `sitemap.cache-control`. When `serve-under-root-path` is false, register the route with `NonApplicationRootPathBuildItem.routeBuilder().route("/robots.txt")`. The absolute path puts it on the host root when the root path is not `/`, and under `/` otherwise. When `serve-under-root-path` is true, register it with `RouteBuildItem.builder().route("/robots.txt")`, which puts it under the root path.

- [ ] **Step 5: Run the tests.** Expected: PASS.

- [ ] **Step 6: Commit.** `git commit -m "Serve robots.txt from rule groups with AI crawler presets"`

---

### Task 12: Switches and conflict detection

**Files:**
- Create: `deployment/src/main/java/org/sitenetsoft/quarkus/seo/deployment/OwnedPaths.java`, `SeoOutputsBuildItem.java`
- Modify: `deployment/.../SeoProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/OwnedPathsTest.java`, `MasterSwitchTest.java`, `FeatureSwitchTest.java`, `StaticRobotsConflictTest.java`, `JaxrsRobotsConflictTest.java`, `RouteSitemapConflictTest.java`

This task assumes spike 3 gave the expected result.

**Interfaces:**
- Produces:
  - `final class OwnedPaths`: `static Set<String> fromIndex(IndexView index)` (JAX-RS and `@Route` paths, normalized with a leading `/` and no trailing `/`), `static boolean ownedStatic(Optional<StaticResourcesBuildItem> statics, String path)`. `StaticResourcesBuildItem` comes from `io.quarkus.vertx.http.deployment.spi` and lists the `META-INF/resources` files of every jar. `ApplicationArchivesBuildItem` misses jars that have no index, so spike 3 of Task 2 replaced it.
  - `fromIndex` reads the annotation names `jakarta.ws.rs.Path`, `io.quarkus.vertx.web.Route` and `io.quarkus.vertx.web.RouteBase` as `DotName` strings, so the deployment module needs no REST or Reactive Routes dependency. It joins the class `@Path` with the method `@Path`, and the `@RouteBase` path with the `@Route` path. It skips interfaces, because REST client interfaces also carry `@Path`.
  - Known limits for the docs: the `@ApplicationPath` and `quarkus.rest.path` prefixes, `@Route` methods with no `path`, and `regex` routes are not detected.
  - `final class SeoOutputsBuildItem extends SimpleBuildItem`: `boolean meta()`, `boolean sitemap()`, `boolean robots()`, `Set<String> ownedByApplication()`. Tasks 7, 8, 10 and 11 read it in place of the raw config switches.

- [ ] **Step 1: Write the failing tests.**
  - `MasterSwitchTest`, with `quarkus.seo.enabled=false`:
    - `/sitemap.xml` is 404 and `/robots.txt` is 404.
    - No `X-Robots-Tag` header appears.
    - `Arc.container().instance(Seo.class).isAvailable()` is false.
  - `FeatureSwitchTest` has three nested `QuarkusUnitTest` classes:
    - With `sitemap.enabled=false`, the sitemap is 404, robots is 200 and has no `Sitemap:` line.
    - With `robots.enabled=false`, robots is 404.
    - With `meta.enabled=false`, the `Seo` bean is absent and there is no header filter.
  - `StaticRobotsConflictTest`: the app ships `META-INF/resources/robots.txt` with `User-agent: test`. GET `/robots.txt` returns that body.
  - `JaxrsRobotsConflictTest`: a resource with `@Path("/robots.txt")` returns `mine`. The body is `mine`.
  - `RouteSitemapConflictTest`: `@Route(path = "/sitemap.xml")` returns `mine`. The body is `mine`, and robots.txt still has the `Sitemap:` line.
  - `OwnedPathsTest`: index a class with `@Path("/a")` and a method with `@Path("b/")`. Expect `/a/b`.

- [ ] **Step 2: Run them.** Expected: FAIL.

- [ ] **Step 3: Implement** one build step that produces `SeoOutputsBuildItem`. Move the conditions of Tasks 7, 8, 10 and 11 to it. Log nothing about owned paths. Plan 2 shows them in the Dev UI.

- [ ] **Step 4: Run the full build.** `mvn -B -ntp -T 4 verify`. Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Step aside for application-owned paths and honour every switch"`

---

### Task 13: Optional caching

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/SeoCaches.java`
- Create in `.runtime`: `SeoOutputCache.java`, `NoSeoOutputCache.java`, `CacheManagerSeoOutputCache.java`
- Modify: `SitemapService`, the robots route handler in `SeoRecorder`, `SeoProcessor`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/CacheWithoutQuarkusCacheTest.java`
- Test: cache-present cases go in Task 14 (`integration-tests`), because the deployment module has no `quarkus-cache`.

This task assumes spike 4 gave the expected result.

**Interfaces:**
- Produces:
  - `interface SeoOutputCache`: `String get(String cacheName, String key, Supplier<String> compute)`, `void invalidateAll(String cacheName)`. Cache names `seo-sitemap` and `seo-robots`. The key is `baseUrl + "|" + page`.
  - `NoSeoOutputCache` always calls `compute`. `CacheManagerSeoOutputCache` injects `@CacheName("seo-sitemap") Cache` and `@CacheName("seo-robots") Cache`. Quarkus creates only the caches it finds by name at build time, so `CacheManager.getCache(name)` alone returns an empty `Optional`. It calls `Cache.get(key, k -> compute.get())` and blocks with `await().indefinitely()` on the worker thread.
  - `@Singleton public class SeoCaches`: `void invalidateSitemap()`, `void invalidateRobots()`.
  - Build step: register `CacheManagerSeoOutputCache` when `Capabilities.isPresent(Capability.CACHE)` and a cache key is enabled. Otherwise register `NoSeoOutputCache`. Name the class as a string. Register it with `AdditionalBeanBuildItem` and also with `AdditionalIndexedClassesBuildItem`, because the cache extension reads `@CacheName` from the combined index.
  - Recorder method `logCacheMissing()`. Call it at runtime init when a cache key is enabled and the capability is absent. It logs one INFO line: `quarkus.seo cache is enabled but quarkus-cache is not present. Serving without a cache.`

- [ ] **Step 1: Write the failing test.** `CacheWithoutQuarkusCacheTest` sets `quarkus.seo.sitemap.cache.enabled=true`. Check that:
  - `/sitemap.xml` is 200.
  - A contributor called twice by two requests counts two calls.
  - The log has the INFO line once. Use `.setLogRecordPredicate`.
  - `SeoCaches.invalidateSitemap()` does not throw.

- [ ] **Step 2: Run it.** Expected: FAIL.

- [ ] **Step 3: Implement** the classes and wire `SitemapService` and the robots handler through `SeoOutputCache`.

- [ ] **Step 4: Run the full build.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Cache the sitemap and robots.txt when quarkus-cache is present"`

---

### Task 14: Sample application, JVM and native tests

**Files:**
- Create: `integration-tests/pom.xml` (copy `quarkus-microformats/integration-tests/pom.xml`, artifactId `quarkus-seo-integration-tests`, with the `native` profile)
- Modify: root `pom.xml` (add module `integration-tests`)
- Create in `integration-tests/src/main/java/org/sitenetsoft/quarkus/seo/it/`: `PageResource.java`, `BlogContributor.java`, `CmsPage.java`
- Create: `integration-tests/src/main/resources/templates/base.html`, `templates/PageResource/page.html`, `application.properties`
- Test: `integration-tests/src/test/java/org/sitenetsoft/quarkus/seo/it/SeoResourceTest.java` (`@QuarkusTest`), `SeoResourceIT.java` (`@QuarkusIntegrationTest extends SeoResourceTest`)

**Interfaces:**
- Consumes: the whole public API.
- Dependencies: `quarkus-seo`, `quarkus-rest`, `quarkus-rest-qute`, `quarkus-cache`, test `quarkus-junit`, `rest-assured`, `assertj-core`.

- [ ] **Step 1: Write the sample app.**
  - `application.properties`: `quarkus.seo.site-url=https://example.org`, `quarkus.seo.site-name=Example`, `quarkus.seo.locales=en,fr`, `quarkus.seo.indexable=true`, `quarkus.seo.organization.name=Example Inc`, `quarkus.seo.sitemap.cache.enabled=true`, `quarkus.seo.robots.ai=block-training`.
  - `PageResource`: `GET /` with `@SitemapEntry` renders `base.html` with `{#seo /}`. `GET /{slug}` uses `Seo` with title, description, image, `Article` JSON-LD and an `fr` alternate.
  - `BlogContributor` returns two URLs with `lastmod`, and counts its calls.

- [ ] **Step 2: Write the tests.**
  - `home`: has `<title>Example</title>`, Organization and WebSite JSON-LD, canonical `https://example.org/`.
  - `page`:
    - has `<title>Hello | Example</title>`, hreflang `en`, `fr` and `x-default`
    - has `og:image` absolute and `article:published_time`
    - has an `Article` node in the JSON-LD
  - `sitemap`: lists `/`, the two blog URLs and their `lastmod`.
  - `sitemapIsCached`: two requests, one contributor call.
  - `cacheIsPerHost`: with `site-url` removed through a second test profile, requests with `Host: a.example` and `Host: b.example` each get their own host in `loc`.
  - `invalidateClearsTheCache`: the resource exposes `POST /invalidate`, which calls `SeoCaches.invalidateSitemap()`. After it, the next sitemap request calls the contributor again.
  - `robots`: has `User-agent: GPTBot` and `Sitemap: https://example.org/sitemap.xml`.

- [ ] **Step 3: Run the JVM tests.** `mvn -B -ntp -pl integration-tests -am verify`. Expected: PASS.

- [ ] **Step 4: Commit, then run native.** Commit first, because a host crash wipes `/tmp` and can lose unsaved work. Run `mvn -B -ntp -pl integration-tests -am verify -Dnative -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`. Run nothing else heavy at the same time. Expected: `SeoResourceIT` passes. If a forked JVM dies with SIGSEGV, SIGILL or SIGABRT, run `journalctl -k | grep -i mce` before you debug the code.

- [ ] **Step 5: Fix any native registration gaps.** Add `ReflectiveClassBuildItem` only for classes the native test proves are needed. Then commit: `git commit -m "Add a sample application tested in JVM and native mode"`.

---

### Task 15: Docs, README and CI

**Files:**
- Create: `docs/pom.xml`, `docs/antora.yml`, `docs/templates/includes/attributes.adoc` (copy from quarkus-microformats, renamed)
- Create: `docs/modules/ROOT/nav.adoc` and the pages `index.adoc`, `meta-tags.adoc`, `json-ld.adoc`, `sitemap.adoc`, `robots.adoc`, `configuration.adoc`, `customizing.adoc`, `integrating.adoc`, `native.adoc`, `testing.adoc`, `releasing.adoc`
- Create: `README.md`, `.github/workflows/ci.yml`, `pages.yml`, `release.yml`, and `bin/*` (copied from quarkus-microformats with names changed)

- [ ] **Step 1: Copy the docs module and the workflows.** Replace `microformats` with `seo` in artifact ids, paths and titles. The CI native job keeps `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`, and the Maven jobs keep `-T 4`.

- [ ] **Step 2: Write the pages.** Each page follows the prose rules. These facts must appear:
  - `index`: add the dependency, set `quarkus.seo.site-url`, put `{#seo /}` in `<head>`.
  - `meta-tags`:
    - the merge order (Java wins over the tag)
    - the `seo:` namespace
    - message bundle titles
    - that `noindex` must be set in Java to reach the `X-Robots-Tag` header
    - the Host-header risk, with the recommendation to set `site-url` or `allowed-hosts` in prod
  - `json-ld`: the builders, `jsonLdRaw` (it must be one JSON object), the site-wide nodes on the home page only.
  - `sitemap`:
    - the three sources and splitting
    - Google ignores `changefreq` and `priority` and trusts only an accurate `lastmod`
    - the extension never sets `lastmod` to the current time
    - contributor streams are closed after use
    - caching and `SeoCaches`
  - `robots`: the rule groups, the AI presets with their token lists, the root-path rule of spec §8.1.
  - `customizing`: every `@DefaultBean` interface and `RobotsCustomizer`.
  - `integrating`: the capability `org.sitenetsoft.seo`, the public contract of spec §11, the quarkus-pha example.
  - `configuration`: include the generated config doc.

- [ ] **Step 3: Write `README.md`** in the quarkus-microformats layout.

- [ ] **Step 4: Build the docs.** `mvn -B -ntp -T 4 package -Pdocs -DskipTests`. Expected: BUILD SUCCESS with no missing includes.

- [ ] **Step 5: Commit.** `git commit -m "Document the extension and add CI, release and pages workflows"`
