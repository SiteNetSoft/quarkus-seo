# quarkus-seo design spikes

Task 2 of the core plan checks the five risks of spec §17 before later tasks build on them.
Each spike ran as a throwaway `QuarkusUnitTest` on Quarkus 3.40.1 in JVM mode.
The spike code is deleted. This page keeps the outcome, the API that worked and the plan changes.

All five spikes give the expected outcome. No fallback applies.
Spikes 1, 3 and 4 found API details that the plan did not name. The plan now names them in Tasks 7, 11, 12 and 13.

Every command ran with `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64` and with forked JVMs on `-XX:TieredStopAtLevel=1`.

## Spike 1: the Qute section helper

**Outcome: expected.**
A `SectionHelperFactory` in the runtime jar works with build-time validation.
It renders a request-scoped bean value from a JAX-RS method, a Reactive Route and a Qute Web page.

**Setup.**
The factory `SpikeSectionFactory` carried `@EngineConfiguration` and handled the tag `{#spike title="x" /}`.
It lived in the real runtime module, so its jar had no index and no `beans.xml`.
Inside `resolve()`, it read a `@RequestScoped` bean through `Arc.container()` when `requestContext().isActive()` was true.
Qute Web came from `io.quarkiverse.qute.web:quarkus-qute-web-deployment:3.4.5`, which resolves against the 3.40.1 BOM.
Its page lived at `templates/pub/web.html` and answered on `/web`.

**API that worked.**
- Register the factory with `AdditionalBeanBuildItem`. The request-scoped bean needs the same item.
- `AdditionalIndexedClassesBuildItem` alone does not work. Qute reads `@EngineConfiguration` classes from `BeanArchiveIndexBuildItem`, and that index holds only bean archives and additional beans.
  With the indexed-classes item only, startup fails with `no section helper found for {#spike title="x" /}`.
  The build itself passes, because Qute validates unknown sections against a placeholder factory.
- A `@TemplateExtension(namespace = ...)` class behaves the same way. With `AdditionalIndexedClassesBuildItem` alone, rendering fails with `No namespace resolver found for [spikens]`. With `AdditionalBeanBuildItem`, `{spikens:hello}` renders.
- Qute passes every named parameter to `BlockInfo.getParameters()` under its own name, known or not. A positional parameter arrives under its index.
  For `{#spike titel="x" 42 /}`, the keys are `titel` and `0`. The value keeps its quotes, so pass it to `block.addExpression(name, value)`.
- Throwing from `initializeBlock` fails the build inside `QuteProcessor.analyzeTemplates`.
  Build the exception with `block.error("unknown parameter [{name}], known: {known}")`, then `.origin(block.getOrigin())` and `.argument(...)`.
  Qute renders the message as a template. A literal brace in the message breaks it, and the raw template text shows instead.

**Evidence.**
- `mvn -B -ntp -o -pl deployment -am verify -Dtest='Spike1*' -Dsurefire.failIfNoSpecifiedTests=false`
- `Spike1AdditionalBeanTest`: 3 tests pass. `/jaxrs`, `/route` and `/web` each return `[spike title=x bean=bean-value]` and `<i>ns-hello</i>`.
- `Spike1UnknownParamTest`: the build fails with `Parser error in template [pub/web.html:1]: unknown spike parameter [titel], known: [title], received keys: [titel, 0]`. The stack trace contains `QuteProcessor.analyzeTemplates`.
- `Spike1IndexedClassesTest` and `Spike1NoRegistrationTest` show the failures quoted above.

**Plan changes.** Task 7 registers both Qute classes with `AdditionalBeanBuildItem`. Step 3 of Task 7 names the parameter keys and the error builder.

## Spike 2: host-root routes

**Outcome: expected.**
With `quarkus.http.root-path=/app`, `/robots.txt` answers 200 and `/app/robots.txt` answers 404.
Branch 1 of spec §8.1 applies to all four host-root files.

**API that worked.**
- `NonApplicationRootPathBuildItem.routeBuilder().route("/robots.txt").handler(handler).build()` produces a `RouteBuildItem`.
- The absolute path makes the builder set the router type to `RouteType.ABSOLUTE_ROUTE`. The route then sits on the main router, outside the root path.
- With the default root path `/`, the same call gives `APPLICATION_ROUTE`, and `/robots.txt` still answers 200. One call covers both cases.
- `RouteBuildItem.builder().route("/plain.txt")` gives an application route. It answers on `/app/plain.txt` and not on `/plain.txt`. This is the call for `serve-under-root-path=true`.
- `RouteBuildItem` has no public constructor that takes a `RouteType`. The non-application builder is the only way to reach `ABSOLUTE_ROUTE`.

**Evidence.**
- `mvn -B -ntp -o -pl deployment -am verify -Dtest='Spike2*' -Dsurefire.failIfNoSpecifiedTests=false`
- `Spike2RootPathTest`: 3 tests pass. The build step printed `routeType=FRAMEWORK_ROUTE routerType=ABSOLUTE_ROUTE absolutePath=/robots.txt httpRoot=/app/`. The requests gave `/robots.txt` 200, `/app/robots.txt` 404, `/app/plain.txt` 200 and `/plain.txt` 404.
- `Spike2DefaultRootTest`: 1 test passes, and the router type is `APPLICATION_ROUTE`.

**Plan changes.** Step 4 of Task 11 names both calls.

## Spike 3: conflict detection

**Outcome: expected.**
A build step sees static resources, `@Path` values and `@Route` values before it produces a `RouteBuildItem`.
It needs no compile dependency on REST or Reactive Routes.

**API that worked.**
- Static resources: `StaticResourcesBuildItem` from `io.quarkus.vertx.http.deployment.spi`, which `quarkus-vertx-http-deployment` already brings.
  Its `getPaths()` lists every `META-INF/resources` file on the runtime class path, such as `/robots.txt`. It covers the application and every jar, indexed or not.
  Consume it as optional, because Quarkus produces it only when a static file exists and no Servlet container is present.
- `ApplicationArchivesBuildItem` is not enough. `getRootArchive().getChildPath("META-INF/resources/robots.txt")` finds application files.
  `getApplicationArchives()` holds only indexed jars, so it misses a file in a jar with no index.
  The spike placed a file in the runtime module, which has no index. `getApplicationArchives()` missed it, and `StaticResourcesBuildItem` found it.
- JAX-RS: `CombinedIndexBuildItem.getIndex().getAnnotations(DotName.createSimple("jakarta.ws.rs.Path"))`.
  A class target gives the class path. A method target gives the method path, joined with the `@Path` of the declaring class.
  `@Path("/jx")` with a method `@Path("sub/")` gave `/jx/sub`. A class `@Path("sitemap.xml")` gave `/sitemap.xml`.
  An interface with `@Path` also appears, because REST client interfaces use the same annotation. Skip interfaces.
- Reactive Routes: `getAnnotations(DotName.createSimple("io.quarkus.vertx.web.Route"))` and the `path` value.
  Join it with the `path` of `io.quarkus.vertx.web.RouteBase` on the declaring class. `@RouteBase(path = "base")` with `@Route(path = "r")` gave `/base/r`.
- The step consumed these items and produced a `RouteBuildItem` with no build cycle.

**Known limits.**
The spike did not cover the `@ApplicationPath` prefix, the `quarkus.rest.path` prefix, `@Route` methods with no `path` and `regex` routes.
Task 12 lists them as known limits in the docs.

**Evidence.**
- `mvn -B -ntp -o -pl deployment -am verify -Dtest='Spike3*' -Dsurefire.failIfNoSpecifiedTests=false`
- `Spike3ConflictsTest`: 1 test passes. The route built from the findings returned `jaxrs=[/jx, /jx/sub, /sitemap.xml, interface /client]`, `routes=[/base/r, /route-owned]` and `staticResourcesBuildItem:paths=[/llms.txt, /robots.txt, /spike-runtime.txt]`.
  It also returned `applicationArchives:spike-runtime.txt=false` and `rootArchive:robots.txt=true`.

**Plan changes.** Task 12 takes `Optional<StaticResourcesBuildItem>` in `OwnedPaths.ownedStatic`. It names the annotation strings, the joins, the interface rule and the known limits.

## Spike 4: optional cache

**Outcome: expected.**
The application starts with and without `quarkus-cache` in JVM mode, with no missing-class error.
Task 14 still has to prove native mode.

**Setup.**
A scratch reactor outside the root build held three modules.
- `lib` had no index. It held `SpikeCache`, `NoSpikeCache` and `CacheManagerSpikeCache`, and it declared `quarkus-cache` as optional.
- `app` had no `quarkus-cache`.
- `app-cache` added `quarkus-cache-deployment`.

A test build step consumed `Capabilities` and named one implementation class as a string.
A Maven profile did not work for the "with cache" case. The Quarkus test bootstrap reads the module POM without the active profile, so two modules replaced the profile.

**API that worked.**
- `Capabilities.isPresent(Capability.CACHE)`, where the capability is `io.quarkus.cache`.
- Register the chosen class by name with `AdditionalBeanBuildItem.builder().addBeanClasses(name).setUnremovable().build()`.
- Without `quarkus-cache`, the class stays in the jar, and the class loader has no `io/quarkus/cache/CacheManager.class`. Startup succeeds.
- Quarkus creates only the caches it finds by name at build time.
  With `CacheManager.getCache("seo-sitemap")` alone, the result is an empty `Optional`. Config keys under `quarkus.cache.caffeine` do not create a cache.
- An injection point `@Inject @CacheName("seo-sitemap") Cache` declares the name.
  The cache extension reads `@CacheName` from the combined index. The class therefore also needs `AdditionalIndexedClassesBuildItem`.
  With both items, `getCacheNames()` returned `[seo-sitemap]`.
- `AdditionalCacheNameBuildItem` in `quarkus-cache-deployment-spi` would also declare the name. It adds a deployment dependency, so the plan uses `@CacheName`.

**Evidence.**
- `mvn -B -ntp -o -f spike-cache/pom.xml verify`
- `app`: 1 test passes. It printed `Capability.CACHE present=false all=[io.quarkus.cdi]` and `describe=no-cache`.
- `app-cache`: 1 test passes. It printed `Capability.CACHE present=true` and `describe=cache-manager:seo-sitemap=computed names=[seo-sitemap] same=true`.
- Before the `@CacheName` change, `app-cache` failed with `NoSuchElementException: No value present` from `getCache`.

**Plan changes.** Task 13 declares both cache names with `@CacheName` injection points. Its build step registers the class by name with both build items.

## Spike 5: the X-Robots-Tag filter

**Outcome: expected.**
The header appears on a static file, a JAX-RS method and a Reactive Route. The filter does not buffer bodies.

**API that worked.**
- `new FilterBuildItem(handler, 100)`. The handler calls `routingContext.addHeadersEndHandler(...)` and then `routingContext.next()`.
- The callback checks `response().headers().contains("X-Robots-Tag")` first. A JAX-RS method that set `X-Robots-Tag: none` kept its value.
- The handler was a plain class with a public no-argument constructor. The bytecode recorder created it with no `@Recorder`.
  Task 8 still uses `SeoRecorder`, because the real filter needs runtime config.

**Evidence.**
- `mvn -B -ntp -o -pl deployment -am verify -Dtest='Spike5*' -Dsurefire.failIfNoSpecifiedTests=false`
- `Spike5RobotsHeaderTest`: 5 tests pass.
- The 1 MB file `META-INF/resources/big.bin` came back with `X-Robots-Tag: noindex, nofollow` and `content-length=1048576`.
  It had no `Transfer-Encoding`, and all 1048576 bytes arrived. The static handler still sends the file directly.
- A chunked route wrote one chunk and ended 1.5 s later. The headers arrived after 66 ms, with the header set. The body ended after 1565 ms. The headers leave before the body, so nothing waits for the full body.
- A 404 response also carries the header.

**Plan changes.** None.
