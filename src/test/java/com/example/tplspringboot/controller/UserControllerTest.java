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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

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

    private String registerAndGetToken(String email, String tenantId) throws Exception {
        String registerRequest = String.format("""
            {
                "name": "Test User",
                "email": "%s",
                "password": "password123"
            }
            """, email);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("accessToken").asText();
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isForbidden()); // 403 - Spring Security is working correctly
    }

    @Test
    void shouldAllowAccessWithValidJWT() throws Exception {
        String token = registerAndGetToken(generateUniqueEmail("user"), "tenant1");

        mockMvc.perform(get("/api/v1/users")
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldRejectMismatchedTenantInJWTAndHeader() throws Exception {
        String token = registerAndGetToken(generateUniqueEmail("user"), "tenant1");

        // Try to use tenant1 token with tenant2 header
        mockMvc.perform(get("/api/v1/users")
                .header("X-Tenant-ID", "tenant2")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()); // 403 - JWT validation rejects mismatched tenant
    }

    @Test
    void shouldCreateUserWithValidData() throws Exception {
        String token = registerAndGetToken(generateUniqueEmail("admin"), "tenant1");
        String newUserEmail = generateUniqueEmail("newuser");

        String createUserRequest = """
            {
                "name": "New User",
                "email": "%s",
                "age": 25
            }
            """.formatted(newUserEmail);

        mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value(newUserEmail))
                .andExpect(jsonPath("$.age").value(25));
    }

    @Test
    void shouldValidateUserCreationData() throws Exception {
        String adminEmail = generateUniqueEmail("admin");
        String token = registerAndGetToken(adminEmail, "tenant1");

        String invalidUserRequest = """
            {
                "name": "",
                "email": "invalid-email",
                "age": -1
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void shouldUpdateExistingUser() throws Exception {
        String token = registerAndGetToken("admin@tenant1.com", "tenant1");

        // Create a user first
        String createUserRequest = """
            {
                "name": "Original User",
                "email": "original@tenant1.com",
                "age": 30
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        var jsonNode = objectMapper.readTree(responseBody);
        Long userId = jsonNode.get("id").asLong();

        // Update the user
        String updateUserRequest = """
            {
                "name": "Updated User",
                "email": "updated@tenant1.com",
                "age": 35
            }
            """;

        mockMvc.perform(put("/api/v1/users/" + userId)
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateUserRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value("updated@tenant1.com"))
                .andExpect(jsonPath("$.age").value(35));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        String adminEmail = generateUniqueEmail("admin");
        String token = registerAndGetToken(adminEmail, "tenant1");

        // Create a user first
        String createUserRequest = """
            {
                "name": "To Delete User",
                "email": "delete@tenant1.com",
                "age": 25
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        var jsonNode = objectMapper.readTree(responseBody);
        Long userId = jsonNode.get("id").asLong();

        // Delete the user
        mockMvc.perform(delete("/api/v1/users/" + userId)
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify user is deleted
        mockMvc.perform(get("/api/v1/users/" + userId)
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldIsolateUsersBetweenTenants() throws Exception {
        // Generate unique emails to avoid conflicts with previous test runs
        String user1Email = generateUniqueEmail("user1");
        String user2Email = generateUniqueEmail("user2");
        
        // Create users in different tenants
        String tenant1Token = registerAndGetToken(user1Email, "tenant1");
        String tenant2Token = registerAndGetToken(user2Email, "tenant2");

        // Create user in tenant1
        String isolatedUserEmail = generateUniqueEmail("isolated");
        String createUserRequest = String.format("""
            {
                "name": "Tenant1 User",
                "email": "%s",
                "age": 30
            }
            """, isolatedUserEmail);

        mockMvc.perform(post("/api/v1/users")
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + tenant1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserRequest))
                .andExpect(status().isCreated());

        // Verify tenant2 cannot see tenant1's users (core multi-tenancy test)
        // Note: May have leftover users from previous test runs, but should not see tenant1's users
        mockMvc.perform(get("/api/v1/users")
                .header("X-Tenant-ID", "tenant2")
                .header("Authorization", "Bearer " + tenant2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1))) // At least the registered user
                .andExpect(jsonPath("$[?(@.email == '" + user1Email + "')]").doesNotExist()) // Doesn't see tenant1 user
                .andExpect(jsonPath("$[?(@.email == '" + isolatedUserEmail + "')]").doesNotExist()); // Doesn't see tenant1's created user

        // Verify tenant1 can see its users and has the expected users
        mockMvc.perform(get("/api/v1/users")
                .header("X-Tenant-ID", "tenant1")
                .header("Authorization", "Bearer " + tenant1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2))) // At least registered user + created user
                .andExpect(jsonPath("$[?(@.email == '" + user1Email + "')]").exists()) // Can see its own user
                .andExpect(jsonPath("$[?(@.email == '" + isolatedUserEmail + "')]").exists()) // Can see its created user
                .andExpect(jsonPath("$[?(@.email == '" + user2Email + "')]").doesNotExist()); // Doesn't see tenant2 user
    }
}
