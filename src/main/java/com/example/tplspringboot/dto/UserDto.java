package com.example.tplspringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User DTO for API requests and responses.
 * Demonstrates validation and OpenAPI documentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information")
public class UserDto {

    @Schema(description = "User ID", example = "123", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(description = "User's full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must not exceed 120")
    @Schema(description = "User's age", example = "25", minimum = "18", maximum = "120")
    private Integer age;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    @Schema(description = "User's phone number", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "Account creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}
