package com.asms.exception;

/**
 * Thrown when authentication fails (HTTP 401).
 *
 * <p>Examples: invalid credentials, expired token, no token provided.
 * Never reveal whether the username or the password was wrong.
 */
public class AuthenticationException extends AsmsException {

    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message);
    }

    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
