package com.adorsysgis.lightbridge.keycloak.client;

/**
 * Raised when a {@code (subject, project_id)} pair cannot be resolved to context. Carries the upstream
 * HTTP status (or {@code -1} for transport/parse failures) so callers can distinguish "unknown request"
 * from "service unavailable".
 */
public class ContextResolutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final int NO_STATUS = -1;

    private final int statusCode;

    public ContextResolutionException(String message) {
        this(message, NO_STATUS, null);
    }

    public ContextResolutionException(String message, int statusCode) {
        this(message, statusCode, null);
    }

    public ContextResolutionException(String message, Throwable cause) {
        this(message, NO_STATUS, cause);
    }

    public ContextResolutionException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    /** True when the Identity Request Service reported the subject as not a member, or the project unknown. */
    public boolean isNotFound() {
        return statusCode == 404;
    }
}
