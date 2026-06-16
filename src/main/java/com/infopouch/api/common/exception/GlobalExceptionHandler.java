package com.infopouch.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /** Security: Always return generic message for auth failures to prevent enumeration attacks */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[{}] Authentication failed: {}", traceId, ex.getClass().getSimpleName());
    // Don't log the actual exception message as it may contain sensitive info

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode("AUTH_001")
            .message("Invalid credentials. Please check your email and password.")
            .status(HttpStatus.UNAUTHORIZED.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  /** Security: Custom auth exception handler */
  @ExceptionHandler(com.infopouch.api.common.exception.AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleCustomAuthException(
      com.infopouch.api.common.exception.AuthenticationException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[{}] Auth operation failed", traceId);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode("AUTH_002")
            .message("Authentication failed. Please verify your credentials.")
            .status(HttpStatus.UNAUTHORIZED.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  /** Security: Resource not found - don't reveal what exists or not */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    log.info("[{}] Resource not found: {}", traceId, ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode("NOT_FOUND_001")
            .message("The requested resource could not be found.")
            .status(HttpStatus.NOT_FOUND.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /** Input validation errors */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    String fieldError =
        ex.getBindingResult().getFieldErrors().isEmpty()
            ? "Validation failed"
            : ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));

    log.debug("[{}] Validation error: {}", traceId, fieldError);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode("VALIDATION_001")
            .message(fieldError)
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /** Custom validation exception */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      ValidationException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    log.debug("[{}] Validation error: {}", traceId, ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode("VALIDATION_002")
            .message(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /** Security: Catch all illegal arguments (but don't expose details) */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[{}] Invalid argument: {}", traceId, ex.getMessage());

    // Determine if it's an auth-related error
    boolean isAuthError =
        ex.getMessage() != null
            && (ex.getMessage().contains("password") || ex.getMessage().contains("verified"));

    String message =
        isAuthError
            ? "Invalid credentials or account status. Please verify your information."
            : "The request contained invalid data. Please review your input.";

    String errorCode = isAuthError ? "AUTH_003" : "VALIDATION_003";

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /** Security: Catch state exceptions (e.g., email already verified) */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    log.warn("[{}] Invalid state: {}", traceId, ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode("STATE_001")
            .message(
                ex.getMessage() != null
                    ? ex.getMessage()
                    : "Operation cannot be performed at this time.")
            .status(HttpStatus.CONFLICT.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /** Security: Generic catch-all for unexpected exceptions - NEVER expose stack trace */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    log.error("[{}] Unexpected error occurred", traceId, ex);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .errorCode("INTERNAL_ERROR")
            .message(
                "An unexpected error occurred. Please contact support with trace ID: " + traceId)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .traceId(traceId)
            .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
