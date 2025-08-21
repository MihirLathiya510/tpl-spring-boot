package com.example.tplspringboot.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails due to invalid credentials.
 * Results in HTTP 401 Unauthorized response.
 */
public class AuthenticationException extends BusinessException {
    
    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message, HttpStatus.UNAUTHORIZED);
    }
    
    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid email or password");
    }
    
    public static AuthenticationException accountDisabled() {
        return new AuthenticationException("User account is disabled");
    }
}
