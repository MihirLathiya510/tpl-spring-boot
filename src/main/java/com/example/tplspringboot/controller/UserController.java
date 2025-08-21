package com.example.tplspringboot.controller;

import com.example.tplspringboot.dto.ErrorResponse;
import com.example.tplspringboot.dto.UserDto;
import com.example.tplspringboot.entity.User;
import com.example.tplspringboot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User management controller with PostgreSQL persistence and RLS-based multi-tenancy.
 * 
 * This controller demonstrates Phase 4 features:
 * - PostgreSQL database persistence
 * - Automatic tenant isolation via Row-Level Security
 * - JPA repository pattern with Spring Data
 * - Transactional operations
 * - Comprehensive API documentation
 * 
 * All operations are automatically tenant-aware through PostgreSQL RLS policies.
 * No manual tenant filtering is required in the application code.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "CRUD operations for user management with automatic tenant isolation")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get all users",
            description = "Retrieves a paginated list of users within the current tenant context. " +
                         "Results are automatically filtered by PostgreSQL Row-Level Security."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - invalid or missing authentication",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        
        log.info("Fetching users - page: {}, size: {}", page, size);
        
        // Create pageable request
        Pageable pageable = PageRequest.of(page, size);
        
        // Get users from database (automatically filtered by RLS)
        Page<User> userPage = userService.findAll(pageable);
        
        // Convert to DTOs
        List<UserDto> userDtos = userPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.info("Retrieved {} users for current tenant (page {}/{})", 
                userDtos.size(), page + 1, userPage.getTotalPages());
        
        return ResponseEntity.ok(userDtos);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user by ID within the current tenant context. " +
                         "Access is automatically restricted to the current tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found in current tenant",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
        
        log.info("Fetching user with ID: {}", id);
        
        User user = userService.findById(id);
        UserDto userDto = convertToDto(user);
        
        log.info("Retrieved user '{}' with ID {}", user.getEmail(), id);
        return ResponseEntity.ok(userDto);
    }

    @Operation(
            summary = "Create new user",
            description = "Creates a new user within the current tenant context. " +
                         "The user will be automatically assigned to the current tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User with email already exists in current tenant",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Creating new user with email: {}", userDto.getEmail());
        
        User user = userService.createUser(
                userDto.getName(),
                userDto.getEmail(),
                userDto.getAge(),
                "defaultPassword123" // In real app, this would be generated or required
        );
        
        UserDto responseDto = convertToDto(user);
        
        log.info("Created user '{}' with ID {}", user.getEmail(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Operation(
            summary = "Update user",
            description = "Updates an existing user within the current tenant context. " +
                         "Only users belonging to the current tenant can be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found in current tenant",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UserDto userDto) {
        
        log.info("Updating user with ID: {}", id);
        
        // Find existing user (automatically filtered by tenant)
        User existingUser = userService.findById(id);
        
        // Update fields
        existingUser.setName(userDto.getName());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setAge(userDto.getAge());
        
        // Save updated user
        User updatedUser = userService.updateUser(existingUser);
        UserDto responseDto = convertToDto(updatedUser);
        
        log.info("Updated user '{}' with ID {}", updatedUser.getEmail(), id);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Delete user",
            description = "Deletes a user by ID within the current tenant context. " +
                         "Only users belonging to the current tenant can be deleted."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found in current tenant",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
        
        log.info("Deleting user with ID: {}", id);
        
        userService.deleteUser(id);
        
        log.info("Successfully deleted user with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Converts User entity to UserDto for API responses.
     */
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .age(user.getAge()) // Map actual age from user entity
                .phoneNumber("+1234567890") // Default phone for demo
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}