package com.asms.exception;

/**
 * Thrown when a state conflict prevents an operation (HTTP 409).
 *
 * <p>Examples: attempting a lifecycle transition that is not allowed,
 * trying to commit an import session that is already BLOCKED.
 */
public class ConflictException extends AsmsException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
