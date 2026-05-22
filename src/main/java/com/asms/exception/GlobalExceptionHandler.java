package com.asms.exception;

import com.asms.model.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/**
 * Global exception handler — translates ASMS domain exceptions and Spring MVC
 * validation failures to the contract's {@link ErrorResponseDto}.
 *
 * <p>HTTP status mapping:
 * <ul>
 *   <li>{@link ResourceNotFoundException}  → 404 NOT FOUND</li>
 *   <li>{@link AccessDeniedException}       → 403 FORBIDDEN</li>
 *   <li>{@link AuthenticationException}     → 401 UNAUTHORIZED</li>
 *   <li>{@link AccountLockedException}      → 423 LOCKED</li>
 *   <li>{@link ConflictException}           → 409 CONFLICT</li>
 *   <li>{@link ValidationException}         → 400 BAD REQUEST</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 BAD REQUEST</li>
 *   <li>All others                          → 500 INTERNAL SERVER ERROR</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        log.debug("Validation error: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto()
            .code("VALIDATION_ERROR")
            .message(ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed"))
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleDomainValidation(ValidationException ex) {
        log.debug("Domain validation error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponseDto error = new ErrorResponseDto()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponseDto error = new ErrorResponseDto()
            .code(ex.getErrorCode())
            .message("Access denied")
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException ex) {
        log.debug("Authentication failure [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccountLocked(AccountLockedException ex) {
        log.debug("Account locked until: {}", ex.getLockedUntil());
        ErrorResponseDto error = new ErrorResponseDto()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(ConflictException ex) {
        log.debug("Conflict [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponseDto error = new ErrorResponseDto()
            .code("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .timestamp(OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
