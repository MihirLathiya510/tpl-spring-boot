package com.example.tplspringboot.exception;

import com.example.tplspringboot.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across the application.
 * Handles both business exceptions and framework exceptions with proper logging and tracing.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles authentication exceptions (invalid credentials).
     */
    @ExceptionHandler(com.example.tplspringboot.exception.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(com.example.tplspringboot.exception.AuthenticationException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Authentication failed [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                null, // details
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    /**
     * Handles business exceptions (custom application exceptions).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Business exception occurred [traceId={}]: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                null, // details
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handles validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Validation exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Input validation failed")
                .details("Please check the provided fields and try again")
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .fieldErrors(fieldErrors)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles bind exceptions from form data validation.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Bind exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Form validation failed")
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .fieldErrors(fieldErrors)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles constraint violation exceptions from Bean Validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Constraint violation exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(this::mapConstraintViolation)
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Constraint validation failed")
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .fieldErrors(fieldErrors)
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Authentication exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "AUTHENTICATION_ERROR",
                "Authentication failed",
                "Please provide valid credentials",
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Access denied exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Access denied",
                "You don't have permission to access this resource",
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles HTTP method not supported exceptions.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Method not supported exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED",
                String.format("HTTP method '%s' is not supported for this endpoint", request.getMethod()),
                String.format("Supported methods: %s", String.join(", ", ex.getSupportedMethods())),
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Handles missing request parameter exceptions.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Missing parameter exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "MISSING_PARAMETER",
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                String.format("Parameter '%s' of type '%s' is required", ex.getParameterName(), ex.getParameterType()),
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles method argument type mismatch exceptions.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Type mismatch exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "TYPE_MISMATCH",
                String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()),
                String.format("Expected type: %s", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"),
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles malformed JSON exceptions.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Message not readable exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_JSON",
                "Invalid JSON format in request body",
                "Please check your JSON syntax and try again",
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles no handler found exceptions (404 errors).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("No handler found exception occurred [traceId={}]: {}", traceId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "ENDPOINT_NOT_FOUND",
                String.format("Endpoint '%s %s' not found", ex.getHttpMethod(), ex.getRequestURL()),
                "Please check the URL and HTTP method",
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles data integrity violation exceptions.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.error("Data integrity violation occurred [traceId={}]: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "DATA_INTEGRITY_VIOLATION",
                "Data integrity constraint violation",
                "The operation violates a database constraint",
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.error("Unexpected exception occurred [traceId={}]: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                "Please try again later or contact support if the problem persists",
                traceId,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maps Spring validation FieldError to our custom FieldError DTO.
     */
    private ErrorResponse.FieldError mapFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .rejectedValue(fieldError.getRejectedValue())
                .message(fieldError.getDefaultMessage())
                .build();
    }

    /**
     * Maps Bean Validation ConstraintViolation to our custom FieldError DTO.
     */
    private ErrorResponse.FieldError mapConstraintViolation(ConstraintViolation<?> violation) {
        return ErrorResponse.FieldError.builder()
                .field(violation.getPropertyPath().toString())
                .rejectedValue(violation.getInvalidValue())
                .message(violation.getMessage())
                .build();
    }

    /**
     * Generates a unique trace ID for request tracking.
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
