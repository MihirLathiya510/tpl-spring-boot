package com.example.tplspringboot.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    void shouldSetAndGetCurrentTenant() {
        // Arrange
        String tenantId = "tenant1";

        // Act
        TenantContext.setCurrentTenant(tenantId);
        String currentTenant = TenantContext.getCurrentTenant();

        // Assert
        assertThat(currentTenant).isEqualTo(tenantId);
    }

    @Test
    void shouldReturnDefaultWhenNoTenantSet() {
        // Act
        String currentTenant = TenantContext.getCurrentTenant();

        // Assert
        assertThat(currentTenant).isEqualTo("default");
    }

    @Test
    void shouldClearTenantContext() {
        // Arrange
        TenantContext.setCurrentTenant("tenant1");

        // Act
        TenantContext.clear();
        String currentTenant = TenantContext.getCurrentTenant();

        // Assert
        assertThat(currentTenant).isEqualTo("default"); // Falls back to default after clear
    }

    @Test
    void shouldIsolateTenantContextBetweenThreads() throws InterruptedException {
        // Arrange
        String mainThreadTenant = "main-tenant";
        String otherThreadTenant = "other-tenant";
        
        TenantContext.setCurrentTenant(mainThreadTenant);
        
        // Create a thread-local variable to capture the result
        final String[] otherThreadResult = new String[1];
        final String[] mainThreadResult = new String[1];

        // Act
        Thread otherThread = new Thread(() -> {
            TenantContext.setCurrentTenant(otherThreadTenant);
            otherThreadResult[0] = TenantContext.getCurrentTenant();
        });

        otherThread.start();
        otherThread.join();

        mainThreadResult[0] = TenantContext.getCurrentTenant();

        // Assert
        assertThat(mainThreadResult[0]).isEqualTo(mainThreadTenant);
        assertThat(otherThreadResult[0]).isEqualTo(otherThreadTenant);
    }

    @Test
    void shouldOverwritePreviousTenant() {
        // Arrange
        String firstTenant = "tenant1";
        String secondTenant = "tenant2";

        // Act
        TenantContext.setCurrentTenant(firstTenant);
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(firstTenant);

        TenantContext.setCurrentTenant(secondTenant);
        String currentTenant = TenantContext.getCurrentTenant();

        // Assert
        assertThat(currentTenant).isEqualTo(secondTenant);
    }
}
