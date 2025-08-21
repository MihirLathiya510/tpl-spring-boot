package com.example.tplspringboot.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when business validation fails.
 * Results in HTTP 400 Bad Request response.
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String field, String value, String reason) {
        super(String.format("Validation failed for field '%s' with value '%s': %s", field, value, reason), 
              "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, cause);
    }
}
