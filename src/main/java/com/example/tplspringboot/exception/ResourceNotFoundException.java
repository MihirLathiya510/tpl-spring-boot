package com.example.tplspringboot.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s with identifier '%s' not found", resourceType, identifier), 
              "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }
}
