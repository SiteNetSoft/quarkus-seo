# Quarkus SEO

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A [Quarkus](https://quarkus.io/) extension that adds search engine and social media output to a web application. It
writes meta tags and [JSON-LD](https://json-ld.org/) into the page head. It serves a sitemap and a robots.txt. You add
the extension, set the site URL and put one tag in your base template. Every part accepts configuration, a replacement
bean or a switch. Status: pre-release. No artifact is on Maven Central yet.

## Features

- One Qute tag, `{#seo /}`, that writes title, description, canonical, hreflang, Open Graph and Twitter tags
- A request scoped `Seo` bean that sets values from Java, which win over the values of the tag
- Typed schema.org builders for JSON-LD, without Jackson
- A sitemap from configuration, from `@SitemapEntry` and from your own beans, with an index above 50,000 URLs
- A robots.txt from rule groups, with presets for AI crawlers
- Opt-in security.txt, web manifest, IndexNow and llms.txt, all off by default
- A Dev UI with an overview and a page check that lists content warnings in dev mode
- No indexing outside the prod profile
- Optional caching with `quarkus-cache`
- Replaceable beans for locales, rendering, the sitemap, robots.txt, the base URL and IndexNow
- Native image support that needs no configuration

## Getting Started

### Installation

Add the extension to your Quarkus application.

#### Maven

```xml
<dependency>
    <groupId>org.sitenetsoft</groupId>
    <artifactId>quarkus-seo</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### Gradle

```kotlin
implementation("org.sitenetsoft:quarkus-seo:0.1.0-SNAPSHOT")
```

### Set Up a Site

Set the public address of the site in `application.properties`:

```properties
quarkus.seo.site-url=https://example.org
quarkus.seo.site-name=Example
```

Put the tag in the `<head>` of your base template:

```html
<head>
  <meta charset="utf-8">
  {#seo /}
</head>
```

The site now serves `/sitemap.xml` and `/robots.txt`. Each page has a title, a canonical link and the social tags.

### Set Values for a Page

```java
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import io.quarkus.qute.TemplateInstance;

import org.sitenetsoft.quarkus.seo.Seo;

@Path("/")
public class PageResource {

    @Inject
    Seo seo;

    @GET
    @Path("{slug}")
    @Produces("text/html")
    public TemplateInstance page(@PathParam("slug") String slug) {
        CmsPage page = cms.get(slug);
        seo.title(page.title()).description(page.summary());
        return Templates.page(page);
    }
}
```

The title template adds the site name. For the title `About us`, the page title is `About us | Example`.

Set `quarkus.seo.site-url` or `quarkus.seo.allowed-hosts` in production. Without them, the extension builds absolute
URLs from the `Host` header of the request. See
[The Host header](docs/modules/ROOT/pages/meta-tags.adoc#the-host-header).

## Building from Source

Building requires **Java 25** and Maven. The artifacts target Java 21. Set `JAVA_HOME` to a JDK 25.

```bash
# Build all modules and run the tests
JAVA_HOME=/path/to/jdk-25 mvn -T 4 verify

# Build the documentation with the generated configuration reference
JAVA_HOME=/path/to/jdk-25 mvn -Pdocs -pl docs -am package
```

## Documentation

Read the [full documentation](docs/modules/ROOT/pages/index.adoc) for these guides:

- [Meta tags](docs/modules/ROOT/pages/meta-tags.adoc)
- [JSON-LD](docs/modules/ROOT/pages/json-ld.adoc)
- [Sitemap](docs/modules/ROOT/pages/sitemap.adoc)
- [robots.txt](docs/modules/ROOT/pages/robots.adoc)
- [Opt-in outputs](docs/modules/ROOT/pages/opt-in.adoc)
- [Dev UI](docs/modules/ROOT/pages/dev-ui.adoc)
- [Customizing](docs/modules/ROOT/pages/customizing.adoc)
- [Integrating from another extension](docs/modules/ROOT/pages/integrating.adoc)
- [Native](docs/modules/ROOT/pages/native.adoc)
- [Testing](docs/modules/ROOT/pages/testing.adoc)
- [Releasing](docs/modules/ROOT/pages/releasing.adoc)
- [Configuration Reference](docs/modules/ROOT/pages/configuration.adoc)

## Compatibility

| Extension Version | Quarkus Version | Java Version |
|---|---|---|
| 0.1.0-SNAPSHOT | 3.40.1 | 21 or later |

## License

This project uses the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
