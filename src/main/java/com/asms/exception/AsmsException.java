package com.asms.exception;

/**
 * Base exception for all ASMS domain exceptions.
 *
 * <p>All domain exceptions carry an {@code errorCode} (a stable machine-readable identifier)
 * and a human-readable {@code message}. HTTP mapping is handled by
 * {@link GlobalExceptionHandler}.
 */
public class AsmsException extends RuntimeException {

    private final String errorCode;

    public AsmsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AsmsException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
