package com.example.tplspringboot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response DTO for consistent API error handling.
 * Provides detailed error information for debugging and client handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response format")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error type/category", example = "VALIDATION_ERROR")
    private String error;

    @Schema(description = "Human-readable error message", example = "Invalid input provided")
    private String message;

    @Schema(description = "Detailed error description", example = "The 'email' field must be a valid email address")
    private String details;

    @Schema(description = "Unique trace ID for request tracking", example = "abc123-def456-ghi789")
    private String traceId;

    @Schema(description = "Timestamp when error occurred")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    @Schema(description = "API path where error occurred", example = "/api/v1/users")
    private String path;

    @Schema(description = "HTTP method used", example = "POST")
    private String method;

    @Schema(description = "List of validation errors (if applicable)")
    private List<FieldError> fieldErrors;

    /**
     * Represents a field-level validation error.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field validation error details")
    public static class FieldError {
        
        @Schema(description = "Field name that failed validation", example = "email")
        private String field;

        @Schema(description = "Rejected value", example = "invalid-email")
        private Object rejectedValue;

        @Schema(description = "Validation error message", example = "must be a well-formed email address")
        private String message;
    }

    /**
     * Creates a simple error response for basic errors.
     */
    public static ErrorResponse of(int status, String error, String message, String traceId) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a detailed error response with path and method information.
     */
    public static ErrorResponse of(int status, String error, String message, String details, 
                                 String traceId, String path, String method) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .details(details)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .build();
    }
}
