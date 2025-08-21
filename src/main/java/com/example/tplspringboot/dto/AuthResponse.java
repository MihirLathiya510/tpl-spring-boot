package com.example.tplspringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Authentication response DTO containing JWT tokens and user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response with JWT tokens")
public class AuthResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Builder.Default
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiration time")
    private LocalDateTime expiresAt;

    @Schema(description = "Authenticated user information")
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserInfo {
        @Schema(description = "User ID", example = "123")
        private Long id;

        @Schema(description = "User's name", example = "John Doe")
        private String name;

        @Schema(description = "User's email", example = "john.doe@tenant1.com")
        private String email;

        @Schema(description = "User's tenant", example = "tenant1")
        private String tenant;

        @Schema(description = "User's roles", example = "[\"USER\", \"ADMIN\"]")
        private String[] roles;
    }
}
