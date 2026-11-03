package org.sitenetsoft.quarkus.seo.runtime;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.DefaultBean;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;
import org.sitenetsoft.quarkus.seo.BaseUrlResolver;

/**
 * Resolve the base URL from the site URL setting or from the request.
 */
@DefaultBean
@Singleton
public class DefaultBaseUrlResolver implements BaseUrlResolver {

    private final SeoConfig config;
    private final SeoSettings settings;

    /**
     * Create the resolver.
     *
     * @param config the SEO configuration
     * @param settings the resolved site settings
     */
    @Inject
    public DefaultBaseUrlResolver(SeoConfig config, SeoSettings settings) {
        this.config = config;
        this.settings = settings;
    }

    @Override
    public String resolve(RoutingContext context) {
        if (config.siteUrl().isPresent() && !config.siteUrl().get().isBlank()) {
            return Urls.absolute(config.siteUrl().get(), "");
        }
        HttpServerRequest request = context.request();
        String scheme = request.scheme() == null ? "http" : request.scheme();
        String authority = authorityOf(request);
        List<String> allowed = config.allowedHosts().orElse(List.of());
        return choose(scheme, authority, allowed, settings.rootPath(), () -> serverAuthority(request));
    }

    /**
     * Choose the host of the base URL.
     *
     * <p>
     * When the request host matches an allowed host, the base uses the text of the allowed entry and never the
     * request authority. The match ignores case and the request port. An entry without a port gives a URL without a
     * port, which is the default port of the scheme.
     *
     * @param scheme the request scheme
     * @param authority the host and port of the request, or null when missing
     * @param allowed the allowed hosts, empty when any safe host is accepted
     * @param rootPath the root path to append
     * @param serverAuthority supplies the server host and port, used when no allowed host exists
     * @return the base URL
     */
    static String choose(String scheme, String authority, List<String> allowed, String rootPath,
            Supplier<String> serverAuthority) {
        boolean safe = authority != null && Urls.isSafeHost(authority);
        if (allowed.isEmpty()) {
            return scheme + "://" + (safe ? authority : serverAuthority.get()) + rootPath;
        }
        if (safe) {
            String host = hostOf(authority.toLowerCase(Locale.ROOT));
            for (String entry : allowed) {
                String clean = entry.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty() && hostOf(clean).equals(host)) {
                    return scheme + "://" + clean + rootPath;
                }
            }
        }
        return scheme + "://" + allowed.get(0).trim().toLowerCase(Locale.ROOT) + rootPath;
    }

    private static String authorityOf(HttpServerRequest request) {
        HostAndPort hostAndPort;
        try {
            hostAndPort = request.authority();
        } catch (RuntimeException e) {
            // Vert.x refuses a malformed Host header. The caller treats it as an unsafe host.
            return null;
        }
        if (hostAndPort == null) {
            return null;
        }
        String host = hostAndPort.host();
        if (host == null || host.isEmpty()) {
            return null;
        }
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        int port = hostAndPort.port();
        return port > 0 ? host + ":" + port : host;
    }

    /**
     * Strip the port from a host and port value.
     */
    private static String hostOf(String authority) {
        int colon = authority.lastIndexOf(':');
        if (colon > 0 && authority.indexOf(']') < colon) {
            return authority.substring(0, colon);
        }
        return authority;
    }

    private static String serverAuthority(HttpServerRequest request) {
        String host = ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class)
                .orElse("localhost");
        if ("0.0.0.0".equals(host) || "::".equals(host) || host.isBlank()) {
            host = "localhost";
        }
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        int port = request.localAddress() == null ? -1 : request.localAddress().port();
        return port > 0 ? host + ":" + port : host;
    }
}
