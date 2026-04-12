package com.rzodeczko.presentation.controller;

import com.rzodeczko.presentation.dto.HealthCheckResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HealthCheckController.
 */
class HealthCheckControllerTest {
    @Test
    void healthCheck_shouldReturnOkStatus() {
        HealthCheckController controller = new HealthCheckController();
        ResponseEntity<HealthCheckResponseDto> response = controller.healthCheck();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("INVOICE SERVICE OK", response.getBody().message());
    }
}

