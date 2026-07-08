package com.adorsysgis.lightbridge.keycloak.client;

import com.adorsysgis.lightbridge.keycloak.common.LightbridgeConfig;
import com.adorsysgis.lightbridge.keycloak.common.ResolvedContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * {@link ContextResolver} backed by the JDK {@link HttpClient}. Uses no third-party HTTP library and
 * relies on the Jackson that Keycloak already ships, so the deployed provider jar stays tiny and free
 * of classloader conflicts.
 */
public final class HttpContextResolver implements ContextResolver {

    private static final Logger LOG = System.getLogger(HttpContextResolver.class.getName());

    private final LightbridgeConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpContextResolver(LightbridgeConfig config) {
        this(config,
                HttpClient.newBuilder()
                        // Pin HTTP/1.1: the resolve call is a one-shot internal JSON POST, and the
                        // JDK client's default HTTP/2 attempts an h2c upgrade over cleartext that
                        // strict intermediaries (some reverse proxies / service meshes) reject with
                        // 502, which would fail resolution closed. Over TLS this is negotiated via
                        // ALPN anyway, so pinning 1.1 costs nothing and removes the footgun.
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(config.requestTimeout())
                        .build(),
                new ObjectMapper());
    }

    public HttpContextResolver(LightbridgeConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResolvedContext resolve(ContextRequest request) throws ContextResolutionException {
        if (!config.isResolverConfigured()) {
            throw new ContextResolutionException("Lightbridge resolver base URL is not configured");
        }

        URI uri = URI.create(trimTrailingSlash(config.resolverBaseUrl()) + config.resolverPath());
        LOG.log(Level.DEBUG, "Resolving Lightbridge context: POST {0} (authMode={1}, subject={2}, project_id={3})",
                uri, config.authMode(), request.subject(), request.projectId());

        HttpResponse<String> response;
        try {
            response = httpClient.send(buildRequest(request), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.log(Level.ERROR, "Lightbridge context-resolution call to " + uri + " failed", e);
            throw new ContextResolutionException("Failed to call context-resolution service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.ERROR, "Interrupted during Lightbridge context-resolution call to " + uri, e);
            throw new ContextResolutionException("Interrupted while calling context-resolution service", e);
        }

        int status = response.statusCode();
        LOG.log(Level.DEBUG, "Lightbridge context-resolution response: HTTP {0}", status);
        if (status == 404) {
            LOG.log(Level.DEBUG, "Lightbridge context-resolution returned 404 for subject={0}, project_id={1} "
                    + "(non-member or unknown project)", request.subject(), request.projectId());
            throw new ContextResolutionException(
                    "subject is not a member of the project, or the project is unknown", 404);
        }
        if (status < 200 || status >= 300) {
            LOG.log(Level.WARNING, "Lightbridge context-resolution service returned HTTP {0}", status);
            throw new ContextResolutionException("Context-resolution service returned HTTP " + status, status);
        }
        ResolvedContext resolved = parse(response.body());
        LOG.log(Level.DEBUG, "Lightbridge context resolved: account_id={0}, project_id={1}",
                resolved.accountId(), resolved.projectId());
        return resolved;
    }

    private HttpRequest buildRequest(ContextRequest request) {
        URI uri = URI.create(trimTrailingSlash(config.resolverBaseUrl()) + config.resolverPath());
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(config.requestTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(writeBody(request), StandardCharsets.UTF_8));
        applyAuth(builder);
        return builder.build();
    }

    private void applyAuth(HttpRequest.Builder builder) {
        switch (config.authMode()) {
            case BEARER -> {
                if (config.bearerToken() != null && !config.bearerToken().isBlank()) {
                    builder.header("Authorization", "Bearer " + config.bearerToken());
                }
            }
            case BASIC -> {
                String raw = orEmpty(config.basicUsername()) + ":" + orEmpty(config.basicPassword());
                String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + encoded);
            }
            case NONE -> {
                // no authentication header
            }
        }
    }

    private String writeBody(ContextRequest request) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("subject", request.subject());
        node.put("project_id", request.projectId());
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new ContextResolutionException("Failed to serialize context-resolution request", e);
        }
    }

    private ResolvedContext parse(String body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ContextResolutionException("Failed to parse context-resolution response", e);
        }
        String accountId = text(root, "account_id");
        String projectId = text(root, "project_id");
        if (accountId == null || projectId == null) {
            throw new ContextResolutionException("Context-resolution response missing account_id/project_id");
        }
        return new ResolvedContext(accountId, projectId);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
