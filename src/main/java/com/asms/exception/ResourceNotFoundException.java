package com.asms.exception;

/**
 * Thrown when a requested resource does not exist or the caller has no access to it.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends AsmsException {

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceType, Object id) {
        super("NOT_FOUND", resourceType + " not found: " + id);
    }
}
