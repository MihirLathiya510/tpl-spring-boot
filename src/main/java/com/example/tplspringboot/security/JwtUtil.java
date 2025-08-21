package com.example.tplspringboot.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for JWT token operations.
 * 
 * Provides methods for generating, validating, and parsing JWT tokens
 * with support for multi-tenancy and role-based access control.
 * 
 * Uses JJWT library with modern security practices:
 * - HS256 algorithm with secure key generation
 * - Configurable expiration times
 * - Tenant-aware claims
 * - Comprehensive error handling
 */
@Component
@Slf4j
public class JwtUtil {
    
    @Value("${jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpirationMs;
    
    @Value("${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private long refreshExpirationMs;
    
    private SecretKey getSigningKey() {
        // Generate a secure key from the secret
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * Generates a JWT token for the authenticated user.
     * 
     * @param authentication the authentication object
     * @param tenantId the tenant identifier
     * @return the generated JWT token
     */
    public String generateToken(Authentication authentication, String tenantId) {
        String username = authentication.getName();
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant", tenantId);
        claims.put("roles", authorities);
        claims.put("type", "access");
        
        return createToken(claims, username, jwtExpirationMs);
    }
    
    /**
     * Generates a refresh token for the user.
     * 
     * @param username the username
     * @param tenantId the tenant identifier
     * @return the generated refresh token
     */
    public String generateRefreshToken(String username, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant", tenantId);
        claims.put("type", "refresh");
        
        return createToken(claims, username, refreshExpirationMs);
    }
    
    /**
     * Creates a JWT token with the specified claims and expiration.
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(expiration, ChronoUnit.MILLIS);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Extracts username from JWT token.
     * 
     * @param token the JWT token
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
    
    /**
     * Extracts tenant ID from JWT token.
     * 
     * @param token the JWT token
     * @return the tenant ID
     */
    public String getTenantFromToken(String token) {
        return (String) getClaimsFromToken(token).get("tenant");
    }
    
    /**
     * Extracts roles from JWT token.
     * 
     * @param token the JWT token
     * @return the roles as comma-separated string
     */
    public String getRolesFromToken(String token) {
        return (String) getClaimsFromToken(token).get("roles");
    }
    
    /**
     * Extracts token type from JWT token.
     * 
     * @param token the JWT token
     * @return the token type (access/refresh)
     */
    public String getTokenTypeFromToken(String token) {
        return (String) getClaimsFromToken(token).get("type");
    }
    
    /**
     * Extracts expiration date from JWT token.
     * 
     * @param token the JWT token
     * @return the expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }
    
    /**
     * Extracts all claims from JWT token.
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
    
    /**
     * Validates JWT token.
     * 
     * @param token the JWT token to validate
     * @param username the expected username
     * @param tenantId the expected tenant ID
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token, String username, String tenantId) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            String tokenTenant = getTenantFromToken(token);
            
            return tokenUsername.equals(username) &&
                   tokenTenant.equals(tenantId) &&
                   !isTokenExpired(token) &&
                   "access".equals(getTokenTypeFromToken(token));
                   
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates refresh token.
     * 
     * @param token the refresh token to validate
     * @param username the expected username
     * @param tenantId the expected tenant ID
     * @return true if refresh token is valid, false otherwise
     */
    public boolean validateRefreshToken(String token, String username, String tenantId) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            String tokenTenant = getTenantFromToken(token);
            
            return tokenUsername.equals(username) &&
                   tokenTenant.equals(tenantId) &&
                   !isTokenExpired(token) &&
                   "refresh".equals(getTokenTypeFromToken(token));
                   
        } catch (Exception e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if JWT token is expired.
     */
    private boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    /**
     * Extracts JWT token from Authorization header.
     * 
     * @param authHeader the Authorization header value
     * @return the JWT token or null if not found
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
