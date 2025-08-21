package com.example.tplspringboot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for business logic exceptions.
 * Provides consistent error handling for business rule violations.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
