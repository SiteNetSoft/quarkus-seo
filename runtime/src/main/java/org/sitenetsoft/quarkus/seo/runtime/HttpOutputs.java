package org.sitenetsoft.quarkus.seo.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Send generated text files with an ETag and a Cache-Control header.
 */
public final class HttpOutputs {

    private HttpOutputs() {
    }

    /**
     * Send the body, or answer 304 when the request already holds the same version.
     *
     * @param context the routing context
     * @param body the response body
     * @param contentType the value of the Content-Type header
     * @param cacheControl the value of the Cache-Control header, or blank to leave it out
     */
    public static void send(RoutingContext context, String body, String contentType, String cacheControl) {
        String etag = etag(body);
        HttpServerResponse response = context.response();
        response.putHeader(HttpHeaders.ETAG, etag);
        if (cacheControl != null && !cacheControl.isBlank()) {
            response.putHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
        }
        if (matches(context.request().getHeader(HttpHeaders.IF_NONE_MATCH), etag)) {
            response.setStatusCode(304).end();
            return;
        }
        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
        response.end(body);
    }

    /**
     * Compute the ETag of a body.
     *
     * @param body the response body
     * @return the first 16 hex characters of the SHA-256 of the UTF-8 body, in double quotes
     */
    static String etag(String body) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
        byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
        return "\"" + HexFormat.of().formatHex(hash, 0, 8) + "\"";
    }

    /**
     * Tell whether an If-None-Match header matches an ETag.
     *
     * <p>
     * The comparison is weak, so a {@code W/} prefix is ignored.
     *
     * @param header the If-None-Match header, or null
     * @param etag the quoted ETag of the current body
     * @return true when the header is {@code *} or lists the ETag
     */
    static boolean matches(String header, String etag) {
        if (header == null || header.isBlank()) {
            return false;
        }
        for (String part : header.split(",")) {
            String candidate = part.trim();
            if (candidate.equals("*")) {
                return true;
            }
            if (candidate.startsWith("W/")) {
                candidate = candidate.substring(2);
            }
            if (candidate.equals(etag)) {
                return true;
            }
        }
        return false;
    }
}
