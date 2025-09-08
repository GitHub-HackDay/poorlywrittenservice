package com.hackday.quota.controller;

import com.hackday.quota.model.ResourceQuota;
import com.hackday.quota.model.QuotaCheckResponse;
import com.hackday.quota.service.QuotaService;
import com.hackday.quota.service.RequestAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for QuotaController
 */
@WebMvcTest(QuotaController.class)
class QuotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuotaService quotaService;

    @MockBean
    private RequestAnalyticsService analyticsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateQuota() throws Exception {
        // Given
        ResourceQuota quota = new ResourceQuota("test-resource", 100, 60);
        when(quotaService.createOrUpdateQuota(any(ResourceQuota.class))).thenReturn(quota);

        // When & Then
        mockMvc.perform(post("/api/v1/quotas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quota)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value("test-resource"))
                .andExpect(jsonPath("$.maxRequests").value(100))
                .andExpect(jsonPath("$.timeWindowSeconds").value(60));
    }

    @Test
    void testGetQuota() throws Exception {
        // Given
        ResourceQuota quota = new ResourceQuota("test-resource", 100, 60);
        when(quotaService.getQuota("test-resource")).thenReturn(Optional.of(quota));

        // When & Then
        mockMvc.perform(get("/api/v1/quotas/test-resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value("test-resource"))
                .andExpect(jsonPath("$.maxRequests").value(100));
    }

    @Test
    void testGetQuotaNotFound() throws Exception {
        // Given
        when(quotaService.getQuota("non-existent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/quotas/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCheckAndConsumeQuotaAllowed() throws Exception {
        // Given
        QuotaCheckResponse response = QuotaCheckResponse.allowed("test-resource", 5, 100, 55);
        when(quotaService.checkAndConsumeQuota("test-resource", 1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/quotas/test-resource/check-and-consume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.resourceId").value("test-resource"))
                .andExpect(jsonPath("$.currentUsage").value(5))
                .andExpect(jsonPath("$.maxAllowed").value(100));
    }

    @Test
    void testCheckAndConsumeQuotaDenied() throws Exception {
        // Given
        QuotaCheckResponse response = QuotaCheckResponse.denied("test-resource", 100, 100, 30);
        when(quotaService.checkAndConsumeQuota("test-resource", 1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/quotas/test-resource/check-and-consume"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.resourceId").value("test-resource"))
                .andExpect(jsonPath("$.currentUsage").value(100));
    }

    @Test
    void testDeleteQuota() throws Exception {
        // Given
        when(quotaService.deleteQuota("test-resource")).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/v1/quotas/test-resource"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteQuotaNotFound() throws Exception {
        // Given
        when(quotaService.deleteQuota("non-existent")).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/quotas/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllResourceIds() throws Exception {
        // Given
        Set<String> resourceIds = Set.of("resource1", "resource2", "resource3");
        when(quotaService.getAllResourceIds()).thenReturn(resourceIds);

        // When & Then
        mockMvc.perform(get("/api/v1/quotas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // Given
        when(quotaService.getAllResourceIds()).thenReturn(Set.of("resource1", "resource2"));

        // When & Then
        mockMvc.perform(get("/api/v1/quotas/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.totalResources").value(2));
    }

    @Test
    void testResetQuotaUsage() throws Exception {
        // Given
        when(quotaService.resetQuotaUsage("test-resource")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/quotas/test-resource/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Quota usage reset successfully"));
    }
}
