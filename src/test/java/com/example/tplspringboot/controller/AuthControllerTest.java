package com.example.tplspringboot.controller;

import com.example.tplspringboot.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;



    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }
    
    /**
     * Generate unique email for each test to avoid conflicts
     */
    private String generateUniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        String uniqueEmail = generateUniqueEmail("test");
        String registerRequest = """
            {
                "name": "Test User",
                "email": "%s",
                "password": "password123"
            }
            """.formatted(uniqueEmail);

        mockMvc.perform(post("/api/v1/auth/register")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.email").value(uniqueEmail));
    }

    @Test
    void shouldRejectRegistrationWithoutTenantId() throws Exception {
        String registerRequest = """
            {
                "name": "Test User",
                "email": "test@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidRegistrationData() throws Exception {
        String registerRequest = """
            {
                "name": "",
                "email": "invalid-email",
                "password": "123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        // First register a user
        String registerRequest = """
            {
                "name": "Login Test User",
                "email": "login@tenant1.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated());

        // Then login with the same credentials
        String loginRequest = """
            {
                "email": "login@tenant1.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value("login@tenant1.com"));
    }

    @Test
    void shouldRejectLoginWithWrongCredentials() throws Exception {
        String loginRequest = """
            {
                "email": "nonexistent@tenant1.com",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldIsolateUsersBetweenTenants() throws Exception {
        // Register user in tenant1
        String uniqueEmail = generateUniqueEmail("user");
        String registerRequest = """
            {
                "name": "Tenant1 User",
                "email": "%s",
                "password": "password123"
            }
            """.formatted(uniqueEmail);

        mockMvc.perform(post("/api/v1/auth/register")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated());

        // Try to login with same email from tenant2 - should fail
        String loginRequest = """
            {
                "email": "%s",
                "password": "password123"
            }
            """.formatted(uniqueEmail);

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Tenant-ID", "tenant2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isNotFound());
    }
}
