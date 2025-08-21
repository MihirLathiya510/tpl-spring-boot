package com.example.tplspringboot.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request conflicts with the current state.
 * Results in HTTP 409 Conflict response.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public ConflictException(String resource, String identifier) {
        super(String.format("%s with identifier '%s' already exists", resource, identifier), 
              "CONFLICT", HttpStatus.CONFLICT);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, "CONFLICT", HttpStatus.CONFLICT, cause);
    }
}
