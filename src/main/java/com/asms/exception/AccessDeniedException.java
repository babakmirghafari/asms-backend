package com.asms.exception;

/**
 * Thrown when a caller attempts to access a resource outside their org context.
 * Maps to HTTP 403. Used as the RISK-002 multi-tenant isolation enforcement point.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public static AccessDeniedException crossTenantAccess(String requestedOrgId) {
        return new AccessDeniedException(
            "Cross-tenant access denied for organization: " + requestedOrgId);
    }
}
