package com.asms.exception;

/**
 * Thrown when input validation fails at the domain/service layer (HTTP 400).
 *
 * <p>Bean Validation (Jakarta Validation) failures are handled separately by
 * {@link GlobalExceptionHandler#handleValidation}. Use this exception for
 * domain-level validation that cannot be expressed via annotations.
 */
public class ValidationException extends AsmsException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
