package com.example.tplspringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration request DTO for creating new user accounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registration request for creating new user account")
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(description = "User's full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User's email address", example = "john.doe@tenant1.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(description = "User's password", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
