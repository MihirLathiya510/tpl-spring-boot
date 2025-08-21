package com.example.tplspringboot.controller;

import com.example.tplspringboot.dto.AuthResponse;
import com.example.tplspringboot.dto.ErrorResponse;
import com.example.tplspringboot.dto.LoginRequest;
import com.example.tplspringboot.dto.RegisterRequest;
import com.example.tplspringboot.entity.User;
import com.example.tplspringboot.exception.ResourceNotFoundException;
import com.example.tplspringboot.exception.ValidationException;
import com.example.tplspringboot.security.JwtUtil;
import com.example.tplspringboot.service.UserService;
import com.example.tplspringboot.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication controller for user registration and login.
 * 
 * Provides endpoints for:
 * - User registration within tenant context
 * - User authentication with JWT token generation
 * - Token refresh (future enhancement)
 * 
 * All operations are tenant-aware and isolated.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration operations")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "Register new user",
            description = "Creates a new user account within the current tenant context. " +
                         "Email must be unique within the tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already exists within tenant",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        // Validate explicit tenant header is provided for registration
        String tenantHeader = httpRequest.getHeader("X-Tenant-ID");
        if (tenantHeader == null || tenantHeader.trim().isEmpty()) {
            throw new ValidationException("X-Tenant-ID header is required for user registration");
        }
        
        String tenantId = TenantContext.getCurrentTenant();
        
        log.info("Registration request for email '{}' in tenant '{}'", request.getEmail(), tenantId);
        
        // Create new user (tenant context is handled automatically)
        User user = userService.createUser(
                request.getName(),
                request.getEmail(),
                request.getAge(),
                request.getPassword()
        );
        
        // Generate JWT tokens
        UsernamePasswordAuthenticationToken authentication = createAuthentication(user);
        String accessToken = jwtUtil.generateToken(authentication, tenantId);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), tenantId);
        
        // Build response
        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(LocalDateTime.now().plusDays(1)) // 24 hours
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .tenant(user.getTenantId())
                        .roles(user.getRoles().toArray(new String[0]))
                        .build())
                .build();
        
        log.info("Successfully registered user '{}' in tenant '{}'", user.getEmail(), tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Login user",
            description = "Authenticates user credentials and returns JWT tokens. " +
                         "Authentication is performed within the current tenant context."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found in tenant",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Validate explicit tenant header is provided for login
        String tenantHeader = httpRequest.getHeader("X-Tenant-ID");
        if (tenantHeader == null || tenantHeader.trim().isEmpty()) {
            throw new ValidationException("X-Tenant-ID header is required for user authentication");
        }
        
        String tenantId = TenantContext.getCurrentTenant();
        
        log.info("Login request for email '{}' in tenant '{}'", request.getEmail(), tenantId);
        
        // Authenticate user (tenant context is handled automatically)
        User user = userService.authenticate(
                request.getEmail(),
                request.getPassword()
        );
        
        // Validate that the user belongs to the current tenant
        if (!tenantId.equals(user.getTenantId())) {
            log.warn("Login attempt for user '{}' from wrong tenant. User tenant: '{}', Request tenant: '{}'", 
                    request.getEmail(), user.getTenantId(), tenantId);
            throw new ResourceNotFoundException("User not found in tenant: " + tenantId);
        }
        
        // Update last login
        userService.updateLastLogin(user);
        
        // Generate JWT tokens
        UsernamePasswordAuthenticationToken authentication = createAuthentication(user);
        String accessToken = jwtUtil.generateToken(authentication, tenantId);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), tenantId);
        
        // Build response
        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(LocalDateTime.now().plusDays(1)) // 24 hours
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .tenant(user.getTenantId())
                        .roles(user.getRoles().toArray(new String[0]))
                        .build())
                .build();
        
        log.info("Successfully authenticated user '{}' in tenant '{}'", user.getEmail(), tenantId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Creates Spring Security Authentication object from User entity.
     */
    private UsernamePasswordAuthenticationToken createAuthentication(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null, // No password in authentication object
                authorities
        );
    }
}
