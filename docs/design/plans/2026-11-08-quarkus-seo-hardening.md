# Quarkus SEO Hardening for 0.1.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the known gaps that plans 1 and 2 left, so that `quarkus-seo` can ship as 0.1.0 with a public API it can keep.

**Architecture:** No new features. Each task fixes one gap listed in PR #1 or PR #2: the public API surface, the IndexNow client lifecycle, the image merge group, the RESTEasy Classic path, untested host-root routes, the Dev UI log set, and stale docs. The spec is changed only where a task changes a rule it states (§5.2 image group, §7.1 REST prefix).

**Tech Stack:** Java 21 bytecode on JDK 25, Maven 3.9, Quarkus 3.40.1, JUnit 5, AssertJ, REST Assured, Jandex.

**Spec:** `docs/design/specs/2026-10-29-quarkus-seo-design.md`, mainly §5.2, §7.1, §8.1, §9 and §12.

**Base:** Plan 2 is on branch `seo-opt-in` (PR #2, based on `seo-core`, PR #1). Start this plan on a new branch `seo-hardening` from `seo-opt-in`. Rebase onto `master` after PRs #1 and #2 merge.

## Global Constraints

- Every constraint in plan 1 (`docs/design/plans/2026-11-02-quarkus-seo-core.md`) and plan 2 (`docs/design/plans/2026-11-03-quarkus-seo-opt-in-and-dev-ui.md`) "Global Constraints" still applies: versions, packages, prose and Javadoc rules, commit messages (one imperative sentence, no trailers), no Jackson, no `beans.xml`, quiet prod.
- Full build: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -B -ntp -T 4 verify`. Focused run: add `-pl runtime` or `-pl deployment -am`, plus `-Dsurefire.failIfNoSpecifiedTests=false -Dtest=<Class>`.
- Host build rule: run only one heavy job at a time. Before any Maven run, check `pgrep -a java` and `pgrep -a native-image`. A running app (`quarkus-run.jar`, a Quarkus dev-mode JVM) is not a build. A Maven or native-image process from any session is a build: wait for it to end. Do not write a wait loop with `pgrep -f` on a pattern that the loop's own command line contains, because it matches itself.
- Native builds always pass `-Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8`, appended to any value the project already sets.
- The public API is the packages `org.sitenetsoft.quarkus.seo` and `org.sitenetsoft.quarkus.seo.schema`. Everything under `.runtime` and `.deployment` is internal. `.schema` imports only `java.*`.
- No behaviour change outside the gap a task names. Existing tests keep passing unchanged, except where a task says to change one.

## Review Focus

- A page where the tag sets `image` and the Java handler sets only `imageAlt` must keep the tag's image and show the Java alt text. Test in Task 3.
- Dev-mode live reload or shutdown while an IndexNow request waits on a slow endpoint must not block for the 30-second request timeout. Test in Task 2.
- An application on Quarkus REST that still has a leftover `quarkus.resteasy.path` must not get that prefix on its sitemap entries. Test in Task 4.
- security.txt served under the root path must give a `Canonical:` line with the root path, because that is where the file now lives. Test in Task 5.
- After the Dev UI checks a 200-URL sitemap several times, browsing a page that has already logged must not log its warnings again. Test in Task 6.

---

### Task 1: Public API surface

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/schema/JsonWriter.java` (class and `string` back to package-private)
- Create: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/Json.java`
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/ManifestWriter.java`, `DefaultIndexNow.java` (use `Json.string`)
- Modify: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/PublicApiTest.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/schema/JsonEscapingParityTest.java`

**Interfaces:**
- Produces: `public final class Json` in `.runtime` with `public static void string(StringBuilder out, String value)`. Javadoc says the class is internal. Its output is byte for byte the same as `JsonWriter.string` for every input.
- `JsonWriter` and all its members are package-private again, as plan 1 specified.

Ruling (user, 2026-11-08): `JsonWriter` is not public API. The cost is one duplicated escape routine of about 20 lines. The parity test keeps the two copies equal. `.schema` cannot call `.runtime`, because `.schema` is JDK-only.

- [ ] **Step 1: Create the branch.** `git switch -c seo-hardening seo-opt-in`, then commit this plan: `git add docs/design/plans/2026-11-08-quarkus-seo-hardening.md && git commit -m "Add the plan for the 0.1.0 hardening"`.

- [ ] **Step 2: Write the failing tests.**
  - `JsonEscapingParityTest` (in package `org.sitenetsoft.quarkus.seo.schema`, so it can call the package-private `JsonWriter.string`). For each of `""`, `"plain"`, `"a\"b"`, `"back\\slash"`, `"</script>"`, `"\u0000\u0001\u001f"`, `"\n\r\t\b\f"`, `"  "`, `"é😀"`, `JsonWriter.string` and `Json.string` give the same text.
  - `PublicApiTest`:
    - also scans `org.sitenetsoft.quarkus.seo.schema`;
    - asserts that the public types in `.schema` are exactly `Article`, `BreadcrumbList`, `JsonLd`, `Organization`, `SchemaNode`, `WebSite`, plus their public nested builder types if any;
    - checks inherited members too: for each public type, check `type.getMethods()` (public, inherited included) as well as the declared protected methods. A public method inherited from a non-public class must not expose a `.runtime.` type.

- [ ] **Step 3: Run them.** `-pl runtime -Dtest='PublicApiTest,JsonEscapingParityTest'`. Expected: the parity test does not compile, because `Json` does not exist. After a stub, `PublicApiTest` fails because `JsonWriter` is public.

- [ ] **Step 4: Implement.** Copy the escape rules of `JsonWriter.string` into `Json.string`. Switch `ManifestWriter` and `DefaultIndexNow` to `Json`. Make `JsonWriter` package-private and remove the public-API wording from its Javadoc. Leave the existing writer tests unchanged: `ManifestWriterTest` and `DefaultIndexNowTest` must still pass.

- [ ] **Step 5: Run the full build.** Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit.** `git commit -m "Keep JsonWriter internal and check the schema package in the public API test"`

---

### Task 2: Close the IndexNow HTTP client

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/DefaultIndexNow.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/DefaultIndexNowTest.java`

**Interfaces:**
- Produces: `@PreDestroy void close()` on `DefaultIndexNow`. It calls `client.shutdownNow()`, which is JDK 21 API. It must not throw.

Ruling: use `shutdownNow()`, not `close()`. `HttpClient.close()` waits for in-flight requests, and a slow endpoint would hold up a live reload for up to the 30-second timeout. A submission that shutdown cuts short is lost, just as it would be if the endpoint failed, and it is logged at DEBUG. If this is wrong, the cost is a lost ping at shutdown.

- [ ] **Step 1: Write the failing tests** in `DefaultIndexNowTest`, with a JDK `HttpServer` on port 0, as the existing tests use:
  - `closeDoesNotWaitForASlowEndpoint`: the handler sleeps 10 seconds. Call `submit("/a")`, then `close()`. `close()` returns within 2 seconds, and the stage completes normally within 2 seconds after that (`toCompletableFuture().get(2, SECONDS)` does not throw).
  - `submitAfterCloseCompletesNormally`: after `close()`, `submit("/a")` returns a stage that completes normally, and the server gets no request.

- [ ] **Step 2: Run them.** `-pl runtime -Dtest=DefaultIndexNowTest`. Expected: FAIL, because `close` does not exist.

- [ ] **Step 3: Implement `close()`.** The existing `RuntimeException` guards in `submit` and `post` already turn a rejected `sendAsync` into a completed stage. Confirm this with the second test. Do not add code for it.

- [ ] **Step 4: Run the tests, then the full build.** Expected: PASS, then BUILD SUCCESS. `IndexNowSubmitTest`, `IndexNowFailureTest` and `IndexNowUnreachableTest` still pass.

- [ ] **Step 5: Commit.** `git commit -m "Shut down the IndexNow client when the bean is destroyed"`

---

### Task 3: Size or alt text alone keeps the lower image

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SeoValues.java` (`copyFrom`)
- Modify: `docs/design/specs/2026-10-29-quarkus-seo-design.md` §5.2, `docs/modules/ROOT/pages/meta-tags.adoc` (the merge-order section)
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/SeoValuesTest.java`, `SeoResolverTest.java`

**Interfaces:**
- No signature changes.

Ruling: the image group is keyed on `image`. A layer that sets `image` replaces the whole group: image, width, height and alt, and the fields it leaves unset become empty. A layer that sets only width, height or alt keeps the lower image and replaces just the fields it sets. The title group stays as it is. This changes the §5.2 sentence "A layer that sets any field of a group replaces the whole group." A reasonable user who sets only `imageAlt` in Java expects to keep the tag's image. Under the current rule the image disappears. If this is wrong, the cost is a stale width or height kept from a lower image. That only happens when a layer sets a size without the image it belongs to.

- [ ] **Step 1: Write the failing tests.**
  - `SeoValuesTest.altAloneKeepsTheLowerImage`: lower is `image("/a.png").imageWidth(100).imageAlt("A")`, upper is `imageAlt("B")`. The merged values have image `/a.png`, width `100` and alt `B`.
  - `SeoValuesTest.sizeAloneKeepsTheLowerImage`: lower is `image("/a.png").imageAlt("A")`, upper is `imageWidth(200).imageHeight(100)`. The merged values have image `/a.png`, width `200`, height `100` and alt `A`.
  - `SeoValuesTest.imageReplacesTheWholeGroup`: lower is `image("/a.png").imageWidth(100).imageAlt("A")`, upper is `image("/b.png")`. The merged values have image `/b.png`, and width and alt are null. This test should already pass, so it guards the rule.
  - `SeoResolverTest.javaAltKeepsTheTagImage`: the tag sets `image=/t.png` and Java sets only `imageAlt("Java alt")`. The resolved image URL ends with `/t.png`, and its alt is `Java alt`.

- [ ] **Step 2: Run them.** `-pl runtime -Dtest='SeoValuesTest,SeoResolverTest'`. Expected: the two "alone" tests and `javaAltKeepsTheTagImage` fail.

- [ ] **Step 3: Implement.** In `copyFrom`:
  - When `source.image != null`, copy all four fields.
  - Otherwise, copy each of `imageWidth`, `imageHeight` and `imageAlt` that is non-null.

- [ ] **Step 4: Update the spec and docs.** Spec §5.2: replace the image half of the group rule with the ruling above, in one or two plain sentences. `meta-tags.adoc`: state the same rule, with a one-line example (tag image, Java alt).

- [ ] **Step 5: Run the full build.** Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit.** `git commit -m "Keep the lower image when a layer sets only its size or alt text"`

---

### Task 4: RESTEasy Classic path prefix

**Files:**
- Modify: `deployment/src/main/java/org/sitenetsoft/quarkus/seo/deployment/SeoProcessor.java` (where it reads `REST_PATH`, around line 658), `RestPaths.java`
- Modify: `docs/design/specs/2026-10-29-quarkus-seo-design.md` §7.1, `docs/modules/ROOT/pages/sitemap.adoc` (the `@SitemapEntry` section)
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/RestPathsTest.java` (new, plain JUnit, no `QuarkusUnitTest`)

**Interfaces:**
- Produces: `static String restPathKey(boolean resteasyClassic)` in `RestPaths`. It returns `"quarkus.resteasy.path"` when the argument is true and `"quarkus.rest.path"` otherwise.
- `SeoProcessor` passes `capabilities.isPresent(Capability.RESTEASY)` and reads that one key. `RestPaths.prefix(IndexView, Optional<String>)` keeps its signature.

Ruling: the extension reads only the key of the REST stack that is present. A leftover `quarkus.resteasy.path` in a Quarkus REST application is ignored, as Quarkus REST ignores it. When several `@ApplicationPath` classes exist, the class whose name sorts first still wins. That order is deterministic, documented in `RestPaths`, and Quarkus itself rejects most such applications. If this is wrong, the cost is a wrong prefix in a Classic application that also has `quarkus.rest.path` set.

- [ ] **Step 1: Write the failing tests** in `RestPathsTest`:
  - `classicReadsTheResteasyKey`: `restPathKey(true)` is `quarkus.resteasy.path`.
  - `restReadsTheRestKey`: `restPathKey(false)` is `quarkus.rest.path`.
  - `applicationPathWins`: index a test class annotated `@ApplicationPath("/v1")` with Jandex's `Indexer`. `prefix(index, Optional.of("/api"))` is `/v1`.
  - `keyApplies`: with an empty index, `prefix(index, Optional.of("/api/"))` is `/api`.
  - `firstSortedClassWins`: index two `@ApplicationPath` classes, `B` with `/b` and `A` with `/a`. The result is `/a`.

- [ ] **Step 2: Run them.** `-pl deployment -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RestPathsTest`. Expected: does not compile, because `restPathKey` does not exist.

- [ ] **Step 3: Implement.** Add the `Capabilities` parameter to the build step that reads the key, if it does not have one. The existing `RestPathTest` (Quarkus REST, `quarkus.rest.path=/api`) must pass unchanged. It also covers the Review Focus line: Quarkus REST reads only `quarkus.rest.path`.

- [ ] **Step 4: Update the spec and docs.** §7.1 and `sitemap.adoc`: under Quarkus REST, the prefix comes from `@ApplicationPath` or `quarkus.rest.path`. Under RESTEasy Classic, it comes from `@ApplicationPath` or `quarkus.resteasy.path`.

- [ ] **Step 5: Run the full build.** Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit.** `git commit -m "Read the RESTEasy Classic path when Classic is the REST stack"`

---

### Task 5: Host-root files under the root path, and shared path constants

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SeoPaths.java` (add the constants)
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/SecurityTxtHandler.java`, `deployment/src/main/java/org/sitenetsoft/quarkus/seo/deployment/SeoProcessor.java`, `deployment/.../devui/SeoDevUiProcessor.java` (use the constants)
- Test: `deployment/src/test/java/org/sitenetsoft/quarkus/seo/deployment/SecurityTxtUnderRootPathTest.java`, `LlmsTxtUnderRootPathTest.java`, `IndexNowKeyUnderRootPathTest.java`

**Interfaces:**
- Produces in `SeoPaths`: `public static final String ROBOTS_TXT = "/robots.txt"`, `SECURITY_TXT = "/.well-known/security.txt"` and `LLMS_TXT = "/llms.txt"`.
- Remove the private copies: `ROBOTS_PATH`, `SECURITY_TXT_PATH` and `LLMS_TXT_PATH` in both processors, and `PATH` in `SecurityTxtHandler`. Callers use the `SeoPaths` names.

- [ ] **Step 1: Write the tests.** Model each one on `RobotsUnderRootPathTest`: `quarkus.http.root-path=/app`, the feature's `serve-under-root-path=true`, `quarkus.seo.indexable=true`, and `given().basePath("")`.
  - `SecurityTxtUnderRootPathTest`: set `security-txt.enabled=true` and `security-txt.contact=mailto:sec@ex.com`.
    - `/app/.well-known/security.txt` answers 200.
    - The body contains `Canonical: http://localhost:` and ends with `/app/.well-known/security.txt\n`.
    - `/.well-known/security.txt` answers 404.
  - `LlmsTxtUnderRootPathTest`: with `llms-txt.enabled=true`, `/app/llms.txt` answers 200 and `/llms.txt` answers 404.
  - `IndexNowKeyUnderRootPathTest`: with `indexnow.enabled=true` and `indexnow.key=abcdef0123456789`, `/app/abcdef0123456789.txt` answers 200 with body `abcdef0123456789`, and `/abcdef0123456789.txt` answers 404.

- [ ] **Step 2: Run them.** `-pl deployment -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*UnderRootPathTest'`. Expected: PASS. These tests pin behaviour that already exists. If one fails, the route rule has a bug: fix it in `hostRootRoute` and say so in the report.

- [ ] **Step 3: Replace the duplicated constants** with the `SeoPaths` names.

- [ ] **Step 4: Run the full build.** Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit.** `git commit -m "Test the host-root files under the root path and share their path constants"`

---

### Task 6: Dev UI log set survives sitemap checks

**Files:**
- Modify: `runtime/src/main/java/org/sitenetsoft/quarkus/seo/runtime/devui/SeoDevCapture.java`
- Test: `runtime/src/test/java/org/sitenetsoft/quarkus/seo/runtime/devui/SeoDevCaptureTest.java` (new)

**Interfaces:**
- Produces: `boolean logged(String path)`, package-private, for tests. It returns true when the path is in the logged set.
- Constants: `MAX_PATHS = 1000` stays for `captured`. A new `MAX_LOGGED = 10_000` bounds `logged`.

Ruling: raise the bound of the logged set, and keep the bound of the captured heads. A logged entry is one key and costs little. A captured `ResolvedSeo` costs more. With 10,000 entries, fifty 200-URL sitemap checks still keep a browsed page's entry. If this is wrong, a page logs its warnings a second time after a very long dev session. That is the same symptom as today, much later.

- [ ] **Step 1: Write the failing tests** in `SeoDevCaptureTest`. Use a minimal `ResolvedSeo`. Copy the fixture from `SeoChecksTest` if it has one.
  - `browsedPathStaysLoggedAfterManyExpects`: `record("/a", seo)`, then `expect("/p" + i)` for i from 0 to 1,499. `logged("/a")` is true.
  - `expectedPathDoesNotLogLater`: `expect("/b")`, then `record("/b", seo)`. `logged("/b")` is true, and `last("/b")` is present.
  - `capturedHeadsStayBounded`: record 1,001 paths. `last` of the first path is empty.

- [ ] **Step 2: Run them.** `-pl runtime -Dtest=SeoDevCaptureTest`. Expected: the first test fails.

- [ ] **Step 3: Implement.** Give `logged` its own bound. Rewrite the `expect` Javadoc to say what it does: it marks the path as logged for the rest of the dev session, so neither the Dev UI fetch nor later browsing logs that path's warnings. The Dev UI shows them instead. Update the class Javadoc to give both bounds.

- [ ] **Step 4: Run the full build.** Expected: BUILD SUCCESS. `DevCaptureLogTest` and `PageCheckLogTest` still pass.

- [ ] **Step 5: Commit.** `git commit -m "Bound the Dev UI log set separately so sitemap checks do not evict browsed pages"`

---

### Task 7: Docs, spec status and release check

**Files:**
- Modify: `docs/modules/ROOT/pages/sitemap.adoc` (alternates bullet), `docs/modules/ROOT/pages/dev-ui.adoc`, `docs/design/specs/2026-10-29-quarkus-seo-design.md` (status line)
- No code.

- [ ] **Step 1: Sitemap self link.** In the alternates bullet of `sitemap.adoc`, add one sentence: the extension skips the extra self link when an alternate already uses the default locale. This matches `DefaultSitemapWriter.writeAlternates`.

- [ ] **Step 2: Dev UI log rule.** In `dev-ui.adoc`, state the rule in one or two sentences. A page check marks the page as shown, and the console does not log that page's warnings later in the session.

- [ ] **Step 3: Spec status.** Change the status line to `implemented (plans 1 to 3)`.

- [ ] **Step 4: Release script test.** Run `bin/test-release-scripts.sh`. Expected: it passes. Do not change the version, tag or deploy. The release is the user's step, done by following `releasing.adoc`.

- [ ] **Step 5: Native IT.** Check the host build rule first. Then run `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 mvn -B -ntp -T 4 verify -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.additional-build-args=-J-XX:TieredStopAtLevel=1,--parallelism=8 -pl integration-tests -am`. Expected: the native IT passes 13/13, as in plan 2. If a JVM or native-image process crashes, run `journalctl -k | grep -i mce` before you debug.

- [ ] **Step 6: Commit.** `git commit -m "Document the sitemap self link and the Dev UI log rule and mark the spec implemented"`
