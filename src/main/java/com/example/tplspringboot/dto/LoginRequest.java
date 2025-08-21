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
 * Login request DTO for user authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request with user credentials")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User's email address", example = "john.doe@tenant1.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "User's password", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
