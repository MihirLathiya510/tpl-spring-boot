package com.example.tplspringboot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "testSecretKeyThatIsAtLeast256BitsLongForHS256AlgorithmTesting");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 604800000L); // 7 days
    }

    @Test
    void shouldGenerateValidAccessToken() {
        // Arrange
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken("test@tenant1.com", null, authorities);
        String tenantId = "tenant1";

        // Act
        String token = jwtUtil.generateToken(authentication, tenantId);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        
        // Verify token content
        String username = jwtUtil.getUsernameFromToken(token);
        String tokenTenant = jwtUtil.getTenantFromToken(token);
        String roles = jwtUtil.getRolesFromToken(token);
        
        assertThat(username).isEqualTo("test@tenant1.com");
        assertThat(tokenTenant).isEqualTo("tenant1");
        assertThat(roles).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        // Arrange
        String username = "test@tenant1.com";
        String tenantId = "tenant1";

        // Act
        String refreshToken = jwtUtil.generateRefreshToken(username, tenantId);

        // Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.split("\\.")).hasSize(3);
        
        // Verify refresh token content
        String tokenUsername = jwtUtil.getUsernameFromToken(refreshToken);
        String tokenTenant = jwtUtil.getTenantFromToken(refreshToken);
        
        assertThat(tokenUsername).isEqualTo(username);
        assertThat(tokenTenant).isEqualTo(tenantId);
    }

    @Test
    void shouldValidateValidToken() {
        // Arrange
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken("test@tenant1.com", null, authorities);
        String tenantId = "tenant1";
        String token = jwtUtil.generateToken(authentication, tenantId);

        // Act & Assert
        assertTrue(jwtUtil.validateToken(token, "test@tenant1.com", "tenant1"));
    }

    @Test
    void shouldRejectTokenWithWrongUsername() {
        // Arrange
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken("test@tenant1.com", null, authorities);
        String tenantId = "tenant1";
        String token = jwtUtil.generateToken(authentication, tenantId);

        // Act & Assert
        assertFalse(jwtUtil.validateToken(token, "wrong@tenant1.com", "tenant1"));
    }

    @Test
    void shouldRejectTokenWithWrongTenant() {
        // Arrange
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken("test@tenant1.com", null, authorities);
        String tenantId = "tenant1";
        String token = jwtUtil.generateToken(authentication, tenantId);

        // Act & Assert
        assertFalse(jwtUtil.validateToken(token, "test@tenant1.com", "tenant2"));
    }

    @Test
    void shouldExtractTokenFromValidBearerHeader() {
        // Arrange
        String bearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";

        // Act
        String token = jwtUtil.extractTokenFromHeader(bearerToken);

        // Assert
        assertThat(token).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature");
    }

    @Test
    void shouldReturnNullForInvalidBearerHeader() {
        // Act & Assert
        assertThat(jwtUtil.extractTokenFromHeader("Invalid header")).isNull();
        assertThat(jwtUtil.extractTokenFromHeader("Basic dGVzdA==")).isNull();
        assertThat(jwtUtil.extractTokenFromHeader("Bearer")).isNull();
        assertThat(jwtUtil.extractTokenFromHeader(null)).isNull();
    }

    @Test
    void shouldHandleExpiredToken() {
        // Arrange - create token with very short expiration
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 1L); // 1 millisecond
        
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken("test@tenant1.com", null, authorities);
        String token = jwtUtil.generateToken(authentication, "tenant1");

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act & Assert
        assertFalse(jwtUtil.validateToken(token, "test@tenant1.com", "tenant1"));
    }

    @Test
    void shouldHandleMultipleRoles() {
        // Arrange
        var authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        var authentication = new UsernamePasswordAuthenticationToken("admin@tenant1.com", null, authorities);
        String token = jwtUtil.generateToken(authentication, "tenant1");

        // Act
        String roles = jwtUtil.getRolesFromToken(token);

        // Assert
        assertThat(roles).contains("ROLE_USER");
        assertThat(roles).contains("ROLE_ADMIN");
        assertThat(roles).contains(","); // Roles should be comma-separated
    }
}
