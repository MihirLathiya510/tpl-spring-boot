package com.example.tplspringboot.security;

import com.example.tplspringboot.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for processing JWT tokens in requests.
 * 
 * This filter intercepts HTTP requests, extracts JWT tokens from the
 * Authorization header, validates them, and sets up the Spring Security
 * context with the authenticated user information.
 * 
 * The filter also ensures tenant context consistency between the JWT
 * token claims and the current tenant context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(request, jwt);
            }
            
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Don't throw exception, let the request continue
            // The security configuration will handle unauthorized access
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extracts JWT token from the request Authorization header.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return jwtUtil.extractTokenFromHeader(bearerToken);
    }
    
    /**
     * Authenticates user based on JWT token.
     */
    private void authenticateUser(HttpServletRequest request, String jwt) {
        try {
            // Extract information from JWT
            String username = jwtUtil.getUsernameFromToken(jwt);
            String tokenTenant = jwtUtil.getTenantFromToken(jwt);
            String currentTenant = TenantContext.getCurrentTenant();
            
            // Validate token against current tenant context
            if (!jwtUtil.validateToken(jwt, username, currentTenant)) {
                log.warn("JWT token validation failed for user '{}' in tenant '{}'", username, currentTenant);
                return;
            }
            
            // Check tenant consistency
            if (!currentTenant.equals(tokenTenant)) {
                log.warn("Tenant mismatch: JWT token tenant '{}' vs current tenant '{}'", 
                        tokenTenant, currentTenant);
                return;
            }
            
            // Extract roles and create authorities
            String roles = jwtUtil.getRolesFromToken(jwt);
            List<SimpleGrantedAuthority> authorities = parseAuthorities(roles);
            
            // Create authentication token
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Successfully authenticated user '{}' for tenant '{}'", username, currentTenant);
            
        } catch (Exception e) {
            log.error("Error authenticating user from JWT: {}", e.getMessage());
        }
    }
    
    /**
     * Parses roles string into list of authorities.
     */
    private List<SimpleGrantedAuthority> parseAuthorities(String roles) {
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> {
                    // Ensure role has ROLE_ prefix for Spring Security
                    if (!role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
                    }
                    return new SimpleGrantedAuthority(role);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT authentication for public endpoints
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/docs") ||
               path.startsWith("/favicon.ico") ||
               path.startsWith("/error");
    }
}
