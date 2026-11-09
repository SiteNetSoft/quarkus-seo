# Quarkus SEO Opt-in Features and Dev UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the four opt-in outputs (security.txt, the web manifest, IndexNow and llms.txt), the content checks and the Dev UI to `quarkus-seo`.

**Architecture:** Each opt-in output follows the plan 1 shape. A build-time switch feeds `SeoOutputsBuildItem`, which also checks if the application owns the path. A plain Vert.x route serves a text body through `HttpOutputs.send`. The three host-root files reuse the robots.txt route rule. One pure class, `SeoChecks`, finds the content warnings. A dev-only bean, `SeoDevCapture`, records each resolved head, logs its warnings once per path, and feeds the Dev UI JSON-RPC service.

**Tech Stack:** Java 21 bytecode on JDK 25, Maven 3.9, Quarkus 3.40.1 (ArC, Vert.x HTTP, Qute, Dev UI), JDK `HttpClient`, JUnit 5, AssertJ, REST Assured, Lit web components for the Dev UI.

**Spec:** `docs/design/specs/2026-10-29-quarkus-seo-design.md`, mainly §8.1, §9, §10 and §12.

**Base:** Plan 1 is on branch `seo-core` (PR #1). Start this plan on a new branch `seo-opt-in` from `seo-core`. Rebase onto `master` after PR #1 merges.

## Global Constraints

- Every constraint in plan 1 (`docs/design/plans/2026-11-02-quarkus-seo-core.md`, "Global Constraints") still applies. That covers versions, packages, prose and Javadoc rules, commit messages, host build rules, no Jackson, no `beans.xml`, and quiet prod.
- Full build: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -B -ntp -T 4 verify`. Focused run: add `-pl deployment -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=<Class>`. Check first that no other heavy build is running (`pgrep -af 'maven|native-image|gradle'`).
- Every opt-in feature is off by default and has a build-time `enabled` key. A disabled feature registers no route, bean or filter.
- Fixed paths: `/.well-known/security.txt`, `/{key}.txt` for IndexNow, and `/llms.txt`. The manifest path is the build-time key `quarkus.seo.manifest.path`, default `/manifest.webmanifest`.
- Host-root rule (spec §8.1, plan 1 spike 2): security.txt, the IndexNow key file and llms.txt use the same route rule as robots.txt. Each one has a build-time `serve-under-root-path` key, default `false`. The manifest sits under the root path, like the sitemap.
- Application files win (spec §10). The ownership check of `SeoProcessor.outputs` covers every new path. As for robots.txt, any match on a host-root path counts as owned, whatever the root path.
- Every generated text output goes through `HttpOutputs.send`. It uses the Cache-Control value of `quarkus.seo.sitemap.cache-control`, like robots.txt.
- Every new replaceable service is an interface with a `@DefaultBean @Singleton` implementation: `IndexNow` gets `DefaultIndexNow`.
- Dev UI code runs only in dev mode. Its beans and its JSON-RPC service are registered from build steps with `onlyIf = IsLocalDevelopment.class`. Content warnings are logged at WARN in dev mode only, once per path. Prod and test logs stay quiet.
- The Dev UI JSON-RPC namespace is the runtime artifactId, `quarkus-seo`.

## Review Focus

- A security.txt with no `expires`, or with an `expires` value that does not parse, must answer 200 with the line left out. It must never answer 500. The Dev UI shows a warning. Test in Task 2.
- An IndexNow endpoint that answers 500, or a port with nothing listening, must not throw to the caller and must not block the caller's thread. The returned stage completes normally. Test in Task 4.
- A page title in llms.txt that holds `]`, `[`, `\`, a newline or `<` must not break the Markdown link list. Test in Task 5.
- The Dev UI page check takes a path from a text field. A value such as `https://evil.example/`, `//evil.example/x` or `x` with no leading slash must never make the application fetch another host. Test in Task 8.
- Manifest and IndexNow JSON values with quotes, a backslash, `</script>` or control characters must stay valid JSON. Test in Task 3.

---

### Task 1: Switches, paths and host-root routes for the opt-in outputs

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SeoBuildConfig.java`
- Modify: `deployment/src/main/java/org/sitenetsoft/quarkus/seo/deployment/SeoOutputsBuildItem.java`, `SeoProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/IndexNowKeyValidationTest.java`, `IndexNowKeyMissingTest.java`, `ManifestPathValidationTest.java`, `OptInDefaultsTest.java`

**Interfaces:**
- Produces in `SeoBuildConfig`:
  - `SecurityTxtBuild securityTxt()`: `boolean enabled()` default `false`, `boolean serveUnderRootPath()` default `false`.
  - `ManifestBuild manifest()`: `boolean enabled()` default `false`, `String path()` default `/manifest.webmanifest`.
  - `IndexNowBuild indexNow()`: `boolean enabled()` default `false`, `Optional<String> key()`, `boolean serveUnderRootPath()` default `false`. Use `@WithName("indexnow")`, so the key prefix is `quarkus.seo.indexnow`. Task 4 uses the same name on `SeoConfig`.
  - `LlmsTxtBuild llmsTxt()`: `boolean enabled()` default `false`, `boolean serveUnderRootPath()` default `false`.
- Produces in `SeoOutputsBuildItem`: `boolean securityTxt()`, `boolean manifest()`, `boolean indexNow()`, `boolean llmsTxt()`, and `Optional<String> indexNowKeyPath()`, which returns `/{key}.txt` when IndexNow is served. The constructor takes the new values after `robots`.
- Produces in `SeoProcessor`:
  - `static void checkIndexNowKey(String key)`, which throws `IllegalStateException` unless the key matches `[a-zA-Z0-9-]{8,128}`.
  - A private helper `RouteBuildItem hostRootRoute(String path, Handler<RoutingContext> handler, boolean underRootPath, SeoRecorder recorder, NonApplicationRootPathBuildItem nonApplicationRootPath)`, taken out of `robotsRoute`. `robotsRoute` uses it.
  - Constants `SECURITY_TXT_PATH = "/.well-known/security.txt"` and `LLMS_TXT_PATH = "/llms.txt"`.

- [ ] **Step 1: Create the branch.** `git switch -c seo-opt-in seo-core`, then commit this plan: `git add docs/design/plans && git commit -m "Add the plan for the opt-in outputs and the Dev UI"`.

- [ ] **Step 2: Write the failing tests.**
  - `IndexNowKeyValidationTest`: with `quarkus.seo.indexnow.enabled=true` and `quarkus.seo.indexnow.key=short`, the build fails. Use `.assertException(e -> assertThat(e).hasMessageContaining("quarkus.seo.indexnow.key"))`.
  - `IndexNowKeyMissingTest`: with `indexnow.enabled=true` and no key, the build fails with a message that holds `quarkus.seo.indexnow.key`.
  - `ManifestPathValidationTest`: with `manifest.enabled=true` and `manifest.path=/a:b`, the build fails with a message that holds `quarkus.seo.manifest.path`.
  - `OptInDefaultsTest`: with no config, `/.well-known/security.txt`, `/manifest.webmanifest` and `/llms.txt` answer 404. `RobotsEndpointTest` and `RobotsRootPathTest` still pass after the refactor.

- [ ] **Step 3: Run them.** Expected: the three validation tests fail, because the build accepts the values.

- [ ] **Step 4: Implement.** In `outputs`, validate the manifest path with `checkPath` and the IndexNow key with `checkIndexNowKey`, but only when each feature is on. Add the security.txt, IndexNow key and llms.txt paths, and the normalized manifest path, to the ownership loop. Move the body of `robotsRoute` into `hostRootRoute`.

  Ruling: a missing or invalid IndexNow key fails the build. The feature is an explicit opt-in, and no fallback key can be right. If this is wrong, the cost is one build failure that names the key. The docs state the rule.

- [ ] **Step 5: Run the full build.** Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit.** `git commit -m "Add the switches, paths and host-root route rule for the opt-in outputs"`

---

### Task 2: security.txt

**Files:**
- Modify: `runtime/.../runtime/SeoConfig.java` (add the group `SecurityTxt securityTxt()`)
- Create in `.runtime`: `SecurityTxtWriter.java`, `SecurityTxtHandler.java`
- Modify: `runtime/.../runtime/Urls.java`, `SeoRecorder.java`, `deployment/.../SeoProcessor.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/SecurityTxtWriterTest.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/SecurityTxtEndpointTest.java`, `SecurityTxtRootPathTest.java`, `SecurityTxtConflictTest.java`, `SecurityTxtNoContactTest.java`

**Interfaces:**
- Consumes: `SeoOutputsBuildItem.securityTxt()`, `hostRootRoute` (Task 1), `BaseUrlResolver`, `SeoSettings.rootPath()`, `HttpOutputs.send`.
- Produces:
  - `SeoConfig.SecurityTxt`: `Optional<List<String>> contact()`, `Optional<String> expires()`, `Optional<String> encryption()`, `Optional<String> policy()`, `Optional<List<String>> preferredLanguages()`, `Optional<String> hiring()`.
  - `final class SecurityTxtWriter`: `static Optional<String> write(SeoConfig.SecurityTxt config, String canonicalUrl)`. The result is empty when no contact is set. It also has `static Optional<OffsetDateTime> expires(String value)`, which Task 6 reuses.
  - `SeoRecorder.securityTxtHandler()`.

- [ ] **Step 1: Write the failing unit tests.**

```java
@Test void fullFileInRfcOrder() {
    // contact=mailto:sec@ex.com,https://ex.com/report expires=2027-01-01T00:00:00Z encryption=https://ex.com/pgp.txt
    // policy=https://ex.com/policy preferred-languages=en,fr hiring=https://ex.com/jobs
    assertThat(write(cfg, "https://ex.com/.well-known/security.txt")).contains(
        "Contact: mailto:sec@ex.com\nContact: https://ex.com/report\nExpires: 2027-01-01T00:00:00Z\n"
        + "Encryption: https://ex.com/pgp.txt\nPolicy: https://ex.com/policy\nPreferred-Languages: en, fr\n"
        + "Hiring: https://ex.com/jobs\nCanonical: https://ex.com/.well-known/security.txt\n");
}
@Test void bareEmailGetsMailto() { /* contact=sec@ex.com -> "Contact: mailto:sec@ex.com" */ }
@Test void dateOnlyExpiresIsStartOfDayUtc() { /* expires=2027-01-01 -> "Expires: 2027-01-01T00:00:00Z" */ }
@Test void offsetIsNormalizedToUtc() { /* 2027-01-01T02:00:00+02:00 -> "Expires: 2027-01-01T00:00:00Z" */ }
@Test void invalidExpiresLeavesTheLineOut() { /* expires=next-year -> no "Expires:" line, no exception */ }
@Test void noContactGivesEmpty() { assertThat(write(noContact, CANON)).isEmpty(); }
@Test void newlineInAValueIsDropped() { /* contact="mailto:a@ex.com\nPolicy: x" -> one Contact line, text after the newline removed */ }
```

- [ ] **Step 2: Write the failing deployment tests.**
  - `SecurityTxtEndpointTest`: with `security-txt.enabled=true`, a contact and `site-url=https://ex.com`, GET `/.well-known/security.txt` answers 200 with `text/plain; charset=utf-8`, an ETag, and a body that ends with `Canonical: https://ex.com/.well-known/security.txt\n`.
  - `SecurityTxtRootPathTest`: with `quarkus.http.root-path=/app` and no site URL, `/.well-known/security.txt` is 200, its Canonical line has no `/app`, and `/app/.well-known/security.txt` is 404.
  - `SecurityTxtConflictTest`: the app ships `META-INF/resources/.well-known/security.txt` with `Contact: mine`. The body is the app's file.
  - `SecurityTxtNoContactTest`: enabled with no contact. The answer is 404.

- [ ] **Step 3: Run them.** Expected: FAIL.

- [ ] **Step 4: Implement.** Add a helper `static String hostRoot(String baseUrl)` to `Urls`. It returns the scheme and authority of the base URL, with no path. The Canonical URL is `hostRoot(baseUrl)` plus the fixed path. When `serve-under-root-path` is true, it is the base URL plus the fixed path. A blank `expires` and a value that does not parse are both left out. Register the route with `hostRootRoute`.

- [ ] **Step 5: Run the tests.** Expected: PASS.

- [ ] **Step 6: Commit.** `git commit -m "Serve security.txt from config"`

---

### Task 3: Web manifest and its link tag

**Files:**
- Modify: `runtime/.../schema/JsonWriter.java` (make the class and `string(StringBuilder, String)` public)
- Modify: `runtime/.../runtime/SeoConfig.java` (add the group `Manifest manifest()`)
- Create in `.runtime`: `ManifestWriter.java`, `ManifestHandler.java`
- Modify: `runtime/.../ResolvedSeo.java`, `runtime/.../runtime/SeoResolver.java`, `runtime/.../runtime/DefaultSeoHeadRenderer.java`, `SeoRecorder.java`, `deployment/.../SeoProcessor.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/ManifestWriterTest.java`, update `DefaultSeoHeadRendererTest.java` and `SeoResolverTest.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/ManifestEndpointTest.java`, `ManifestLinkTest.java`, `ManifestConflictTest.java`

**Interfaces:**
- Consumes: `SeoOutputsBuildItem.manifest()`, `SeoSettings.siteName()`, `SeoSettings.rootPath()`, `SeoConfig.themeColor()`.
- Produces:
  - `SeoConfig.Manifest`: `Optional<String> name()`, `Optional<String> shortName()`, `Optional<String> startUrl()`, `Optional<Display> display()`, `Optional<String> themeColor()`, `Optional<String> backgroundColor()`, `Map<String, Icon> icons()`. `enum Display { FULLSCREEN, STANDALONE, MINIMAL_UI, BROWSER }`. `Icon` has `String src()`, `Optional<String> sizes()`, `Optional<String> type()`.
  - `final class ManifestWriter`: `static String write(SeoConfig.Manifest config, Optional<String> siteName, Optional<String> themeColor, String rootPath)`.
  - `ResolvedSeo` gets a last component `Optional<String> manifest`, the href of the manifest link.
  - `public record ManifestLink(Optional<String> href)` in `.runtime`. It is a synthetic singleton, recorded at build time whenever meta is on. The href is the root path plus the manifest path when the extension serves the manifest. Otherwise it is empty.
  - `SeoResolver` gets `ManifestLink` as a new constructor parameter. Keep the current four-parameter constructor for the unit tests, with an empty link.

- [ ] **Step 1: Write the failing unit tests.**

```java
@Test void fullManifestInKeyOrder() {
    // name=Ex short-name=E start-url=/start display=minimal-ui theme-color=#123 background-color=#fff
    // icons.a.src=/i/192.png icons.a.sizes=192x192 icons.a.type=image/png, root path /app
    assertThat(write(...)).isEqualTo("{\"name\":\"Ex\",\"short_name\":\"E\",\"start_url\":\"/app/start\","
        + "\"display\":\"minimal-ui\",\"theme_color\":\"#123\",\"background_color\":\"#fff\","
        + "\"icons\":[{\"src\":\"/app/i/192.png\",\"sizes\":\"192x192\",\"type\":\"image/png\"}]}");
}
@Test void nameFallsBackToSiteNameAndThemeColorToTheSiteKey() { }
@Test void startUrlDefaultsToTheRootPath() { /* root /app, no start-url -> "start_url":"/app/" */ }
@Test void absoluteIconUrlIsKept() { /* https://cdn.ex.com/i.png stays as it is */ }
@Test void iconsSortedByName() { }
@Test void valuesAreJsonEscaped() { /* name = a"b\c</script>\u0001 -> valid JSON, "<\/script>" form as JsonWriter writes it */ }
@Test void emptyKeysAreLeftOut() { /* no name and no site name -> no "name" key */ }
// DefaultSeoHeadRendererTest
@Test void manifestLinkAfterThemeColor() { /* manifest=/app/manifest.webmanifest -> <link rel="manifest" href="/app/manifest.webmanifest"> after the theme-color meta */ }
```

- [ ] **Step 2: Write the failing deployment tests.**
  - `ManifestEndpointTest`: with `manifest.enabled=true`, GET `/manifest.webmanifest` answers 200 with `application/manifest+json`, an ETag, and a body that holds `"name":`.
  - `ManifestLinkTest`: a Qute page with `{#seo /}` holds `<link rel="manifest" href="/manifest.webmanifest">`. A second class sets `manifest.path=/site.webmanifest` and `quarkus.http.root-path=/app`. The href is `/app/site.webmanifest`.
  - `ManifestConflictTest`: the app ships `META-INF/resources/manifest.webmanifest`. GET returns the app's body, and `{#seo /}` writes no manifest link.

- [ ] **Step 3: Run them.** Expected: FAIL.

- [ ] **Step 4: Implement.** A path that starts with `/` gets the root path in front. A value with a scheme stays as it is. Make the `.schema` `JsonWriter` public with Javadoc. `PublicApiTest` does not scan `.schema`, so it needs no change.

  Ruling: `{#seo}` writes the manifest link only when the extension serves the manifest. An application that ships its own manifest also writes its own link. If this is wrong, the cost is one line in the application's template.

- [ ] **Step 5: Run the tests.** Expected: PASS.

- [ ] **Step 6: Commit.** `git commit -m "Serve the web manifest and link it from the seo tag"`

---

### Task 4: IndexNow

**Files:**
- Modify: `runtime/.../runtime/SeoConfig.java` (add `@WithName("indexnow") IndexNowConfig indexNow()` with `@WithDefault("https://api.indexnow.org/indexnow") String endpoint()`)
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/IndexNow.java`
- Create in `.runtime`: `DefaultIndexNow.java`, `IndexNowKey.java`, `IndexNowKeyHandler.java`
- Modify: `SeoRecorder.java`, `deployment/.../SeoProcessor.java`, `runtime/src/test/java/org/sitenetsoft/quarkus/seo/PublicApiTest.java` (add `IndexNow` to the expected types)
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/DefaultIndexNowTest.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/IndexNowKeyFileTest.java`, `IndexNowSubmitTest.java`, `IndexNowNotIndexableTest.java`, `IndexNowFailureTest.java`

**Interfaces:**
- Consumes: `SeoOutputsBuildItem.indexNow()` and `indexNowKeyPath()`, `hostRootRoute`, `SeoSettings.indexable()`, `SeoConfig.siteUrl()`, `JsonWriter.string` (Task 3).
- Produces:
  - `public interface IndexNow`: `CompletionStage<Void> submit(String... paths)` and `CompletionStage<Void> submit(Collection<String> paths)`. The stage always completes normally.
  - `IndexNowKey`, a synthetic singleton recorded at build time: `String key()`, `boolean underRootPath()`.
  - `DefaultIndexNow` is `@DefaultBean @Singleton`. Its package-private constructor `DefaultIndexNow(HttpClient client, String endpoint, IndexNowKey key, SeoSettings settings, Optional<String> siteUrl)` serves the unit tests.
  - `static List<String> body(String host, String key, String keyLocation, List<String> urls)` in `DefaultIndexNow`. It returns the JSON bodies, with at most 10,000 URLs each.

- [ ] **Step 1: Write the failing unit tests.**

```java
@Test void bodyHoldsHostKeyKeyLocationAndUrls() {
    assertThat(body("ex.com", "abcd1234", "https://ex.com/abcd1234.txt", List.of("https://ex.com/a")))
        .containsExactly("{\"host\":\"ex.com\",\"key\":\"abcd1234\",\"keyLocation\":\"https://ex.com/abcd1234.txt\","
            + "\"urlList\":[\"https://ex.com/a\"]}");
}
@Test void splitsAt10000Urls() { /* 10,001 URLs -> two bodies of 10,000 and 1 */ }
@Test void relativePathsUseTheSiteUrlAndOtherHostsAreDropped() { /* /a -> https://ex.com/a, https://other.com/b dropped */ }
@Test void duplicatesKeepTheFirstPosition() { }
@Test void noSiteUrlDoesNothing() { /* stage completes, no request sent */ }
```

- [ ] **Step 2: Write the failing deployment tests.** Each test app has a Reactive Route at `/fake-indexnow`. The route records each request body and answers 202. Set `quarkus.seo.indexnow.endpoint=http://localhost:${quarkus.http.test-port:8081}/fake-indexnow`, `site-url=https://ex.com`, `indexable=true` and `key=abcd1234`.
  - `IndexNowKeyFileTest`: GET `/abcd1234.txt` answers 200 with body `abcd1234` and `text/plain; charset=utf-8`.
  - `IndexNowSubmitTest`: `submit("/a", "/b").toCompletableFuture().get(10, SECONDS)`. The fake endpoint got one body whose `urlList` is `["https://ex.com/a","https://ex.com/b"]`, and whose `Content-Type` is `application/json; charset=utf-8`.
  - `IndexNowNotIndexableTest`: with `indexable=false`, `submit("/a")` completes and the fake endpoint got nothing.
  - `IndexNowFailureTest`: the fake route answers 500. A second test points the endpoint at `http://localhost:1/x`. In both, `submit` returns in under 100 ms on the test thread, and the stage completes normally within 15 seconds.

- [ ] **Step 3: Run them.** Expected: FAIL.

- [ ] **Step 4: Implement.** Use one JDK `HttpClient` with a 10 second connect timeout. Each request has a 30 second timeout. Send with `sendAsync`, so the calling thread never waits. Log a status other than 200 or 202, and every exception, at DEBUG. The key location is `Urls.hostRoot(siteUrl)` (Task 2) plus `/{key}.txt`, or the site URL plus `/{key}.txt` when `serve-under-root-path` is true.

  Ruling: `submit` needs `quarkus.seo.site-url`. A background job has no request, so there is no safe host to use. Without the key, `submit` does nothing and logs at DEBUG. If this is wrong, the cost is that sites without `site-url` can't use IndexNow. The docs state the rule.

- [ ] **Step 5: Run the tests.** Expected: PASS.

- [ ] **Step 6: Commit.** `git commit -m "Serve the IndexNow key file and submit changed URLs"`

---

### Task 5: llms.txt

**Files:**
- Modify: `runtime/.../runtime/SitemapService.java` (make `collect()` public as `List<SitemapUrl> urls()`)
- Create in `.runtime`: `LlmsTxtWriter.java`, `LlmsTxtHandler.java`
- Modify: `SeoRecorder.java`, `deployment/.../SeoProcessor.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/LlmsTxtWriterTest.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/LlmsTxtEndpointTest.java`, `LlmsTxtWithoutSitemapTest.java`, `LlmsTxtRootPathTest.java`

**Interfaces:**
- Consumes: `SeoOutputsBuildItem.llmsTxt()`, `SitemapService`, `SitemapEntries`, `SeoSettings.siteName()`, `SeoConfig.description()`, `hostRootRoute`.
- Produces:
  - `SitemapService.urls()`: the config URLs, the `@SitemapEntry` URLs and the contributor URLs, in that order. It throws `ContributorException` like `page`.
  - `final class LlmsTxtWriter`: `static String write(String heading, Optional<String> description, List<SitemapUrl> urls, String baseUrl)`.

- [ ] **Step 1: Write the failing unit tests.**

```java
@Test void headingQuoteAndLinks() {
    assertThat(write("Ex", Optional.of("Shop for tools."), List.of(SitemapUrl.of("/a").title("A page"), SitemapUrl.of("/b")), "https://ex.com"))
        .isEqualTo("# Ex\n\n> Shop for tools.\n\n- [A page](https://ex.com/a)\n");
}
@Test void noDescriptionLeavesTheQuoteOut() { }
@Test void markdownInTitlesIsEscaped() { /* title "a]b[c\\d<e" -> "- [a\\]b\\[c\\\\d&lt;e](...)" */ }
@Test void newlinesInTitleAndDescriptionBecomeSpaces() { }
@Test void parenthesisInUrlIsPercentEncoded() { /* /a(b) -> https://ex.com/a%28b%29 */ }
```

- [ ] **Step 2: Write the failing deployment tests.**
  - `LlmsTxtEndpointTest`: with `llms-txt.enabled=true`, `site-name=Ex` and a contributor that adds `/a` with title `A page`, GET `/llms.txt` answers 200 with `text/plain; charset=utf-8` and holds `- [A page](http://localhost:8081/a)`. A contributor that throws gives 500, like the sitemap.
  - `LlmsTxtWithoutSitemapTest`: with `sitemap.enabled=false` and `llms-txt.enabled=true`, `/sitemap.xml` is 404 and `/llms.txt` still lists the contributor URL.
  - `LlmsTxtRootPathTest`: with `quarkus.http.root-path=/app`, `/llms.txt` is 200 and its links start with `http://localhost:8081/app/`.

- [ ] **Step 3: Run them.** Expected: FAIL.

- [ ] **Step 4: Implement.** With no site name, the heading is the base URL. Register `SitemapService`, `DefaultSitemapWriter`, the scanner step and the `SitemapEntries` bean when the sitemap or llms.txt is served. Register the sitemap routes only when the sitemap is served.

  Ruling: llms.txt is served when the site is not indexable, as the sitemap is. robots.txt already keeps crawlers out. If this is wrong, the cost is that a staging site shows its page list to anyone who asks. Plan 1 accepted the same cost for the sitemap.

  Ruling: llms.txt has no output cache. If this is wrong, the cost is that slow contributors run on every request. ETag and Cache-Control still apply.

- [ ] **Step 5: Run the tests.** Expected: PASS.

- [ ] **Step 6: Commit.** `git commit -m "Serve llms.txt from the sitemap sources"`

---

### Task 6: Content checks

**Files:**
- Create in `.runtime`: `SeoChecks.java`, `SeoWarning.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/SeoChecksTest.java`

**Interfaces:**
- Consumes: `ResolvedSeo`, `SecurityTxtWriter.expires` (Task 2).
- Produces:
  - `public record SeoWarning(String code, String message)`.
  - `final class SeoChecks`:
    - `static List<SeoWarning> page(ResolvedSeo seo)`
    - `static List<SeoWarning> oneWayHreflang(String pageUrl, List<ResolvedSeo.Link> links, Map<String, List<ResolvedSeo.Link>> alternatesByUrl)`
    - `static List<SeoWarning> duplicateTitles(Map<String, String> titleByUrl)`
    - `static List<SeoWarning> securityTxt(SeoConfig.SecurityTxt config, Instant now)`
  - Codes: `missing-title`, `missing-description`, `long-title`, `description-length`, `missing-image`, `relative-image`, `one-way-hreflang`, `duplicate-title`, `security-txt-expires-missing`, `security-txt-expires-invalid`, `security-txt-expires-soon`, `security-txt-expired`.

- [ ] **Step 1: Write the failing tests.**

```java
@Test void cleanPageHasNoWarnings() { /* title 30 chars, description 100 chars, image https://ex.com/i.png */ }
@Test void missingTitleAndDescription() { /* codes missing-title, missing-description */ }
@Test void titleOver60Characters() { /* 61 chars -> long-title; 60 chars -> none */ }
@Test void descriptionOutside50To160() { /* 49 and 161 -> description-length; 50 and 160 -> none */ }
@Test void missingAndRelativeImage() { /* no image -> missing-image; "/i.png" -> relative-image */ }
@Test void hreflangWithoutReturnLink() { /* page A lists B, B lists only itself -> one-way-hreflang naming B */ }
@Test void hreflangNotFetchedIsNotReported() { /* B absent from the map -> no warning */ }
@Test void duplicateTitlesNameEveryUrl() { /* /a and /b share "Home" -> one warning listing both */ }
@Test void securityTxtExpiry() {
    // now 2027-01-01T00:00:00Z: expires 2027-01-20 -> expires-soon; 2026-12-31 -> expired;
    // 2027-03-01 -> none; unset -> expires-missing; "soon" -> expires-invalid
}
```

- [ ] **Step 2: Run them.** Expected: FAIL.

- [ ] **Step 3: Implement.** Count title length in code points. The title is the full `<title>` text. Messages are one plain sentence each, such as `The title has 72 characters. Keep it at 60 or fewer.`

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Check page metadata and security.txt for content problems"`

---

### Task 7: Dev-mode capture and warning log

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/devui/SeoDevCapture.java`
- Modify: `runtime/.../runtime/SeoResolver.java`
- Create: `deployment/src/main/java/org/sitenetsoft/quarkus/seo/deployment/devui/SeoDevUiProcessor.java`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/devui/DevCaptureLogTest.java`, `NoDevCaptureInTestTest.java`

**Interfaces:**
- Consumes: `SeoChecks.page`, `SeoChecks.securityTxt`, `RequestInfo.path()`.
- Produces:
  - `@Singleton public class SeoDevCapture`:
    - `void record(String path, ResolvedSeo seo)`. It stores the last result per path and logs each warning at WARN the first time it sees that path.
    - `Optional<ResolvedSeo> last(String path)`
    - `void clear(String path)`
  - `SeoResolver` takes `Instance<SeoDevCapture>` and calls `record` after `resolve` when the bean is resolvable and a request is present. The unit-test constructor passes no capture.
  - `SeoDevUiProcessor` has `@BuildSteps(onlyIf = IsLocalDevelopment.class)`. It registers `SeoDevCapture` when meta is on. At start, it logs the security.txt warnings once when security.txt is served.

- [ ] **Step 1: Write the failing tests.**
  - `DevCaptureLogTest` (`QuarkusDevModeTest`): a Qute page with `{#seo title="x" /}` and no description. Request it twice. The log holds one `missing-description` line for `/page`, not two. After the test edits the template to add a 100-character description, the next request logs nothing new.
  - `NoDevCaptureInTestTest` (`QuarkusUnitTest`): `Arc.container().instance(SeoDevCapture.class).isAvailable()` is false. After a page request, no log record at WARN or above comes from `org.sitenetsoft.quarkus.seo`.

- [ ] **Step 2: Run them.** Expected: FAIL.

- [ ] **Step 3: Implement.** The log line is `SEO check on <path>: <message> [<code>]`. Keep at most 1,000 paths in the capture, and drop the oldest first.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** `git commit -m "Record resolved heads and log content warnings in dev mode"`

---

### Task 8: Dev UI card, pages and JSON-RPC service

**Files:**
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/devui/SeoJsonRpcService.java`, `PageFetcher.java`
- Modify: `deployment/.../devui/SeoDevUiProcessor.java`, `deployment/pom.xml` (test dependency `io.quarkus:quarkus-devui-test-spi`)
- Create: `deployment/src/main/resources/dev-ui/qwc-seo-overview.js`, `qwc-seo-page-check.js`
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/devui/SeoDevUiJsonRpcTest.java`, `PageFetcherTest.java` (runtime unit test in `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/devui/`)

**Interfaces:**
- Consumes: `SeoOutputsBuildItem`, `SeoDevCapture`, `SeoChecks`, `SeoHeadRenderer`, `SitemapService.urls()` when it is registered, `SeoSettings`, `SeoLocales`, `SeoConfig`.
- Produces:
  - Card `SEO` with two pages, `Overview` (`qwc-seo-overview`) and `Page check` (`qwc-seo-page-check`). Build-time data `outputs`: a list of `{name, path, servedBy}`, where `servedBy` is `extension`, `application` or `off`, for meta, sitemap, robots.txt, security.txt, manifest, IndexNow key and llms.txt.
  - `SeoJsonRpcService` JSON-RPC methods:
    - `Map<String, Object> getOverview()`: site URL or `request host`, site name, locales, default locale, indexable, and the security.txt warnings.
    - `Map<String, Object> checkPage(String path)`: `status`, `head` (the rendered tags), `snippet` `{title, url, description}`, `card` `{title, description, image, siteName}`, and `warnings`.
    - `Map<String, Object> checkSitemap()`: `checked` (count), `skipped` (count), and `warnings` (duplicate titles plus one entry per URL with page warnings).
  - `final class PageFetcher`: `static Optional<String> localPath(String input)` and `int fetch(String path)`. `fetch` sends GET to `http://localhost:{quarkus.http.port}{root path}{path}` and returns the status.

- [ ] **Step 1: Write the failing tests.**

```java
// PageFetcherTest
@Test void acceptsPlainPaths() { assertThat(localPath("/a/b?x=1")).contains("/a/b?x=1"); }
@Test void refusesOtherHosts() {
    assertThat(localPath("https://evil.example/")).isEmpty();
    assertThat(localPath("//evil.example/x")).isEmpty();
    assertThat(localPath("x")).isEmpty();
    assertThat(localPath("/\\evil.example")).isEmpty();
    assertThat(localPath("/a\r\nHost: evil")).isEmpty();
}
```

  `SeoDevUiJsonRpcTest` extends `DevUIJsonRPCTest` with namespace `quarkus-seo` and uses `QuarkusDevModeTest`. The app has a page `/page` with `{#seo title="Hello" /}` and a contributor that lists `/page` and `/page2`. Both pages have the title `Hello`.
  - `checkPage` with `{"path": "/page"}`: `status` is 200, `head` holds `<title>Hello`, and `warnings` holds `missing-description`.
  - `checkPage` with `{"path": "https://evil.example/"}`: the result has an `error` field and no `status`.
  - `checkSitemap`: `warnings` holds one `duplicate-title` entry that names `/page` and `/page2`.
  - `getOverview`: `indexable` is false.

- [ ] **Step 2: Run them.** Expected: FAIL.

- [ ] **Step 3: Implement the service.** `checkPage` clears the capture for the path, fetches it, then reads `SeoDevCapture.last`. It fetches up to 20 same-site hreflang targets for `oneWayHreflang`. `checkSitemap` fetches at most 200 URLs on the site and counts the rest as `skipped`. Fetches use one JDK `HttpClient` with a 10 second timeout and never follow redirects. `localPath` accepts only a value that starts with exactly one `/` and holds no backslash, no control character and no whitespace.

  Ruling: the page check fetches the page over HTTP and reads the head that `SeoDevCapture` recorded. It does not parse HTML. If this is wrong, the cost is that a page rendered without `{#seo}` shows "no SEO head found".

- [ ] **Step 4: Write the two web components.** Copy the import and `JsonRpc` pattern from a Quarkus 3.40.1 extension (for example `quarkus-caffeine` or `quarkus-agroal`, under `dev-ui/` in their deployment jars). `qwc-seo-overview` shows the overview values, the outputs table with a link for each served path, and the security.txt warnings. `qwc-seo-page-check` has a path field and a Check button. It shows the warnings first, then a search snippet, a social card and the head tags in a `<pre>`, and a "Check sitemap" button for `checkSitemap`. Render every value as text, never as HTML.

- [ ] **Step 5: Run the tests.** Expected: PASS. Then start the sample app in dev mode, open `http://localhost:8080/q/dev-ui`, and check both pages by hand.

- [ ] **Step 6: Commit.** `git commit -m "Add the SEO Dev UI with an overview and a page check"`

---

### Task 9: Sample application and native test

**Files:**
- Modify: `integration-tests/src/main/resources/application.properties`, `integration-tests/src/main/java/org/sitenetsoft/quarkus/seo/it/PageResource.java`
- Test: `integration-tests/src/test/java/org/sitenetsoft/quarkus/seo/it/OptInOutputsTest.java`, `OptInOutputsIT.java`

**Interfaces:**
- Consumes: every output of Tasks 2 to 5.

- [ ] **Step 1: Write the failing tests.** Turn on all four features in the sample application. Use the IndexNow key `sample-key-1234` and point the endpoint at a sample route `/fake-indexnow`. `OptInOutputsTest` checks:
  - security.txt is 200 with a Canonical line.
  - The manifest is 200 with `application/manifest+json`.
  - `/sample-key-1234.txt` is 200.
  - `/llms.txt` lists the `BlogContributor` URLs that have a title.
  - The page head has the manifest link.
  - A POST to a sample endpoint `/publish` calls `IndexNow.submit`. The fake endpoint then gets the URL.

  `OptInOutputsIT extends OptInOutputsTest`.

- [ ] **Step 2: Run the JVM tests.** `mvn -B -ntp -T 4 verify`. Expected: PASS.

- [ ] **Step 3: Commit, then run the native test once.** Commit first: `git commit -m "Cover the opt-in outputs in the sample application"`. Check that no other heavy build runs. Then run `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -B -ntp -T 4 verify -Dnative -pl integration-tests -am -Dquarkus.native.container-build=true -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`. Expected: every IT passes. A `HttpClient` failure in native mode shows as a missing-class or reflection error. If one appears, fix it in the runtime and commit.

---

### Task 10: Docs and spec

**Files:**
- Create: `docs/modules/ROOT/pages/opt-in.adoc`, `docs/modules/ROOT/pages/dev-ui.adoc`
- Modify: `docs/modules/ROOT/nav.adoc`, `docs/modules/ROOT/pages/customizing.adoc` (the `IndexNow` bean), `docs/modules/ROOT/pages/index.adoc` (a short opt-in list), `README.md`
- Modify: `docs/design/specs/2026-10-29-quarkus-seo-design.md`

- [ ] **Step 1: Write `opt-in.adoc`.** It has one section per feature, with a config example, the output for that example, and the rules:
  - the host-root rule and `serve-under-root-path`
  - a missing or invalid IndexNow key fails the build
  - `submit` needs `site-url` and does nothing when the site is not indexable
  - security.txt answers 404 with no contact
  - the manifest link appears only when the extension serves the manifest
  - llms.txt has no cache, and no search engine is known to use it (spec §9)

- [ ] **Step 2: Write `dev-ui.adoc`.** Describe the two pages, every warning code from Task 6 and the dev-mode log line. State that prod and test stay quiet.

- [ ] **Step 3: Update the spec.** Add the plan 1 fix-wave rulings to §3, §5.2, §7.3 and §10:
  - the capability only with meta on
  - the cache only with a pinned host
  - the merge groups
  - the JAX-RS prefixes

  Add this plan's rulings to §9 and §12. Set the status line to `implemented (plans 1 and 2)`.

- [ ] **Step 4: Build the docs.** `mvn -B -ntp -Pdocs -pl docs package`. Expected: BUILD SUCCESS, with no broken xref warnings.

- [ ] **Step 5: Run the full build.** Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit.** `git commit -m "Document the opt-in outputs and the Dev UI"`
