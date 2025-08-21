package com.example.tplspringboot.service;

import com.example.tplspringboot.entity.User;
import com.example.tplspringboot.exception.AuthenticationException;
import com.example.tplspringboot.exception.ConflictException;
import com.example.tplspringboot.exception.ResourceNotFoundException;
import com.example.tplspringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing user operations with PostgreSQL persistence and RLS-based multi-tenancy.
 * 
 * This service leverages PostgreSQL Row-Level Security for automatic tenant isolation.
 * All database operations are automatically filtered by the current tenant context.
 * 
 * Key Features:
 * - Automatic tenant isolation via PostgreSQL RLS
 * - Transactional data operations
 * - BCrypt password encryption
 * - Comprehensive error handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Creates a new user account within the current tenant context.
     * PostgreSQL RLS automatically handles tenant isolation.
     * 
     * @param name the user's name
     * @param email the user's email (must be unique within tenant)
     * @param password the user's plain text password
     * @return the created user
     * @throws ConflictException if email already exists within the current tenant
     */
    public User createUser(String name, String email, String password) {
        // Check if user already exists (RLS automatically filters by tenant)
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("User", email);
        }
        
        // Encrypt password
        String encryptedPassword = passwordEncoder.encode(password);
        
        // Create user entity (tenant_id will be set automatically by BaseEntity)
        User user = User.createNew(name, email, encryptedPassword);
        
        // Save user (RLS ensures it's saved to current tenant)
        User savedUser = userRepository.save(user);
        
        log.info("Created new user '{}' with ID {}", email, savedUser.getId());
        return savedUser;
    }
    
    /**
     * Creates a new user account with age within the current tenant context.
     * PostgreSQL RLS automatically handles tenant isolation.
     * 
     * @param name the user's name
     * @param email the user's email (must be unique within tenant)
     * @param age the user's age
     * @param password the user's plain text password
     * @return the created user
     * @throws ConflictException if email already exists within the current tenant
     */
    public User createUser(String name, String email, Integer age, String password) {
        // Check if user already exists (RLS automatically filters by tenant)
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("User", email);
        }
        
        // Encrypt password
        String encryptedPassword = passwordEncoder.encode(password);
        
        // Create user entity (tenant_id will be set automatically by BaseEntity)
        User user = User.createNew(name, email, age, encryptedPassword);
        
        // Save user (RLS ensures it's saved to current tenant)
        User savedUser = userRepository.save(user);
        
        log.info("Created new user '{}' with age {} and ID {}", email, age, savedUser.getId());
        return savedUser;
    }
    
    /**
     * Finds a user by email within the current tenant context.
     * PostgreSQL RLS automatically filters by tenant.
     * 
     * @param email the user's email
     * @return the user if found
     * @throws ResourceNotFoundException if user not found in current tenant
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }
    
    /**
     * Finds a user by email within the current tenant context, returning Optional.
     * 
     * @param email the user's email
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Authenticates a user by email and password within the current tenant context.
     * PostgreSQL RLS automatically handles tenant isolation.
     * 
     * @param email the user's email
     * @param password the user's plain text password
     * @return the authenticated user
     * @throws AuthenticationException if authentication fails
     */
    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        // Use Optional to avoid throwing ResourceNotFoundException for non-existent users
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            log.warn("Authentication failed for user '{}': user not found", email);
            throw AuthenticationException.invalidCredentials();
        }
        
        User user = userOptional.get();
        
        if (!user.isEnabled()) {
            log.warn("Authentication failed for user '{}': account disabled", email);
            throw AuthenticationException.accountDisabled();
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Authentication failed for user '{}': invalid password", email);
            throw AuthenticationException.invalidCredentials();
        }
        
        log.debug("Successfully authenticated user '{}' with ID {}", email, user.getId());
        return user;
    }
    
    /**
     * Updates user's last login time and saves to database.
     * The updated_at field is automatically updated by JPA auditing.
     * 
     * @param user the user to update
     */
    public void updateLastLogin(User user) {
        // The updated_at field will be automatically updated by JPA auditing
        userRepository.save(user);
        log.debug("Updated last login for user '{}' with ID {}", user.getEmail(), user.getId());
    }
    
    /**
     * Checks if a user exists by email within the current tenant context.
     * PostgreSQL RLS automatically handles tenant filtering.
     * 
     * @param email the email to check
     * @return true if user exists in current tenant, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Finds a user by ID within the current tenant context.
     * 
     * @param id the user ID
     * @return the user if found
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
    }
    
    /**
     * Updates an existing user within the current tenant context.
     * 
     * @param user the user to update
     * @return the updated user
     */
    public User updateUser(User user) {
        User savedUser = userRepository.save(user);
        log.info("Updated user '{}' with ID {}", savedUser.getEmail(), savedUser.getId());
        return savedUser;
    }
    
    /**
     * Deletes a user by ID within the current tenant context.
     * 
     * @param id the user ID to delete
     * @throws ResourceNotFoundException if user not found
     */
    public void deleteUser(Long id) {
        User user = findById(id);
        userRepository.delete(user);
        log.info("Deleted user '{}' with ID {}", user.getEmail(), id);
    }
    
    /**
     * Finds all users within the current tenant context.
     * Uses explicit tenant filtering for reliable isolation.
     * 
     * @return List of all users for the current tenant
     */
    @Transactional(readOnly = true)
    public java.util.List<User> findAll() {
        String currentTenant = com.example.tplspringboot.tenant.TenantContext.getCurrentTenant();
        java.util.List<User> users = userRepository.findAllByTenantId(currentTenant);
        log.debug("Found {} users for tenant '{}'", users.size(), currentTenant);
        
        // Debug: Log tenant IDs of returned users
        if (log.isDebugEnabled()) {
            users.forEach(user -> 
                log.debug("User ID: {}, Email: {}, Tenant: {}", 
                    user.getId(), user.getEmail(), user.getTenantId()));
        }
        
        return users;
    }
    
    /**
     * Finds all users with pagination within the current tenant context.
     * Uses explicit tenant filtering for reliable isolation.
     * 
     * @param pageable pagination information
     * @return Page of users for the current tenant
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) {
        String currentTenant = com.example.tplspringboot.tenant.TenantContext.getCurrentTenant();
        return userRepository.findAllByTenantId(currentTenant, pageable);
    }
}
