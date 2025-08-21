package com.example.tplspringboot.repository;

import com.example.tplspringboot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations with automatic tenant isolation.
 * 
 * This repository leverages PostgreSQL Row-Level Security (RLS) for automatic
 * tenant data isolation. All queries are automatically filtered by the current
 * tenant context set in PostgreSQL session variables.
 * 
 * Key Features:
 * - Automatic tenant filtering via PostgreSQL RLS
 * - No need for explicit tenant_id in query conditions
 * - Thread-safe tenant isolation
 * - Optimized queries with proper indexing
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email within the current tenant context.
     * RLS automatically filters results to the current tenant.
     * 
     * @param email the user's email address
     * @return Optional containing the user if found within current tenant
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by email and enabled status within the current tenant context.
     * 
     * @param email the user's email address
     * @param enabled the enabled status
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailAndEnabled(String email, boolean enabled);

    /**
     * Checks if a user exists with the given email within the current tenant context.
     * 
     * @param email the email to check
     * @return true if user exists in current tenant, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds all users within the current tenant context with pagination.
     * RLS automatically limits results to the current tenant.
     * 
     * @param pageable pagination information
     * @return Page of users for the current tenant
     */
    @Override
    Page<User> findAll(Pageable pageable);

    /**
     * Finds users by name containing the given string (case-insensitive).
     * Results are automatically filtered to the current tenant.
     * 
     * @param name the name substring to search for
     * @param pageable pagination information
     * @return Page of users matching the name criteria
     */
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Finds users by enabled status within the current tenant context.
     * 
     * @param enabled the enabled status to filter by
     * @param pageable pagination information
     * @return Page of users with the specified enabled status
     */
    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    /**
     * Counts the total number of users in the current tenant.
     * RLS automatically limits count to the current tenant.
     * 
     * @return total number of users in current tenant
     */
    @Override
    long count();

    /**
     * Counts users by enabled status in the current tenant.
     * 
     * @param enabled the enabled status to count
     * @return number of users with the specified enabled status
     */
    long countByEnabled(boolean enabled);

    /**
     * Custom query to find users with specific roles.
     * This demonstrates how to write custom queries that work with RLS.
     * 
     * @param role the role to search for
     * @param pageable pagination information
     * @return Page of users having the specified role
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r = :role")
    Page<User> findByRole(@Param("role") String role, Pageable pageable);

    /**
     * Custom query to find recently created users.
     * Demonstrates date-based queries with RLS.
     * 
     * @param days number of days to look back
     * @param pageable pagination information
     * @return Page of users created within the specified number of days
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= CURRENT_TIMESTAMP - :days DAY")
    Page<User> findRecentUsers(@Param("days") int days, Pageable pageable);

    /**
     * Custom native query example showing direct PostgreSQL integration.
     * Note: Native queries also respect RLS policies automatically.
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailNative(@Param("email") String email);
    
    /**
     * Explicit tenant-aware query to find all users by tenant ID.
     * This bypasses RLS and explicitly filters by tenant for debugging.
     * 
     * @param tenantId the tenant ID to filter by
     * @return List of users for the specified tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    java.util.List<User> findAllByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * Explicit tenant-aware query to find all users by tenant ID with pagination.
     * This bypasses RLS and explicitly filters by tenant for debugging.
     * 
     * @param tenantId the tenant ID to filter by
     * @param pageable pagination information
     * @return Page of users for the specified tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    Page<User> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
}
