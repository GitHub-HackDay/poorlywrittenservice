package com.hackday.quota.service;

import com.hackday.quota.model.ResourceQuota;
import com.hackday.quota.model.QuotaCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InMemoryQuotaService focusing on high concurrency scenarios
 */
@ExtendWith(MockitoExtension.class)
class InMemoryQuotaServiceTest {

    private InMemoryQuotaService quotaService;

    @BeforeEach
    void setUp() {
        quotaService = new InMemoryQuotaService(new SimpleMeterRegistry());
    }

    @Test
    void testCreateAndRetrieveQuota() {
        // Given
        ResourceQuota quota = new ResourceQuota("test-resource", 100, 60);

        // When
        ResourceQuota created = quotaService.createOrUpdateQuota(quota);
        
        // Then
        assertNotNull(created);
        assertEquals("test-resource", created.getResourceId());
        assertEquals(100, created.getMaxRequests());
        assertEquals(60, created.getTimeWindowSeconds());

        // Verify retrieval
        var retrieved = quotaService.getQuota("test-resource");
        assertTrue(retrieved.isPresent());
        assertEquals(quota.getResourceId(), retrieved.get().getResourceId());
    }

    @Test
    void testQuotaEnforcement() {
        // Given
        ResourceQuota quota = new ResourceQuota("test-resource", 5, 60);
        quotaService.createOrUpdateQuota(quota);

        // When & Then - First 5 requests should be allowed
        for (int i = 0; i < 5; i++) {
            QuotaCheckResponse response = quotaService.checkAndConsumeQuota("test-resource", 1);
            assertTrue(response.isAllowed(), "Request " + (i + 1) + " should be allowed");
            assertEquals(i + 1, response.getCurrentUsage());
        }

        // 6th request should be denied
        QuotaCheckResponse response = quotaService.checkAndConsumeQuota("test-resource", 1);
        assertFalse(response.isAllowed(), "6th request should be denied");
        assertEquals(5, response.getCurrentUsage());
    }

    @Test
    void testConcurrentQuotaChecks() throws InterruptedException {
        // Given
        ResourceQuota quota = new ResourceQuota("concurrent-test", 1000, 60);
        quotaService.createOrUpdateQuota(quota);

        int threadCount = 100;
        int requestsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        QuotaCheckResponse response = quotaService.checkAndConsumeQuota("concurrent-test", 1);
                        if (response.isAllowed()) {
                            allowedCount.incrementAndGet();
                        } else {
                            deniedCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");
        
        int totalRequests = threadCount * requestsPerThread;
        assertEquals(totalRequests, allowedCount.get() + deniedCount.get(), 
                    "Total processed requests should match expected");
        
        // Should allow exactly up to the quota limit
        assertTrue(allowedCount.get() <= 1000, "Should not exceed quota limit");
        assertTrue(allowedCount.get() >= 900, "Should allow most requests up to quota"); // Some tolerance for timing

        executor.shutdown();
    }

    @Test
    void testMultipleResourcesConcurrently() throws InterruptedException {
        // Given
        int resourceCount = 100;
        int requestsPerResource = 50;
        
        // Create quotas for multiple resources
        for (int i = 0; i < resourceCount; i++) {
            ResourceQuota quota = new ResourceQuota("resource-" + i, requestsPerResource, 60);
            quotaService.createOrUpdateQuota(quota);
        }

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(resourceCount);
        AtomicInteger totalAllowed = new AtomicInteger(0);

        // When - Test each resource concurrently
        for (int i = 0; i < resourceCount; i++) {
            final String resourceId = "resource-" + i;
            executor.submit(() -> {
                try {
                    int allowed = 0;
                    for (int j = 0; j < requestsPerResource + 10; j++) { // Try more than quota
                        QuotaCheckResponse response = quotaService.checkAndConsumeQuota(resourceId, 1);
                        if (response.isAllowed()) {
                            allowed++;
                        }
                    }
                    totalAllowed.addAndGet(allowed);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertTrue(latch.await(15, TimeUnit.SECONDS), "All resource tests should complete");
        
        // Each resource should allow exactly its quota
        int expectedTotal = resourceCount * requestsPerResource;
        assertEquals(expectedTotal, totalAllowed.get(), 
                    "Total allowed requests should equal sum of all quotas");

        executor.shutdown();
    }

    @Test
    void testResourceNotFound() {
        // When
        QuotaCheckResponse response = quotaService.checkAndConsumeQuota("non-existent", 1);

        // Then
        assertFalse(response.isAllowed());
        assertEquals("Resource not found", response.getMessage());
        assertEquals("non-existent", response.getResourceId());
    }

    @Test
    void testResetQuotaUsage() {
        // Given
        ResourceQuota quota = new ResourceQuota("reset-test", 5, 60);
        quotaService.createOrUpdateQuota(quota);

        // Consume all quota
        for (int i = 0; i < 5; i++) {
            quotaService.checkAndConsumeQuota("reset-test", 1);
        }

        // Verify quota is exhausted
        QuotaCheckResponse response = quotaService.checkAndConsumeQuota("reset-test", 1);
        assertFalse(response.isAllowed());

        // When
        boolean reset = quotaService.resetQuotaUsage("reset-test");

        // Then
        assertTrue(reset);
        
        // Should be able to consume quota again
        response = quotaService.checkAndConsumeQuota("reset-test", 1);
        assertTrue(response.isAllowed());
        assertEquals(1, response.getCurrentUsage());
    }

    @Test
    void testDeleteQuota() {
        // Given
        ResourceQuota quota = new ResourceQuota("delete-test", 10, 60);
        quotaService.createOrUpdateQuota(quota);

        // Verify quota exists
        assertTrue(quotaService.getQuota("delete-test").isPresent());

        // When
        boolean deleted = quotaService.deleteQuota("delete-test");

        // Then
        assertTrue(deleted);
        assertFalse(quotaService.getQuota("delete-test").isPresent());

        // Deleting non-existent quota should return false
        assertFalse(quotaService.deleteQuota("delete-test"));
    }

    @Test
    void testGetAllResourceIds() {
        // Given
        quotaService.createOrUpdateQuota(new ResourceQuota("resource1", 10, 60));
        quotaService.createOrUpdateQuota(new ResourceQuota("resource2", 20, 120));

        // When
        var resourceIds = quotaService.getAllResourceIds();

        // Then
        assertEquals(2, resourceIds.size());
        assertTrue(resourceIds.contains("resource1"));
        assertTrue(resourceIds.contains("resource2"));
    }

    @Test
    void testCheckQuotaWithoutConsuming() {
        // Given
        ResourceQuota quota = new ResourceQuota("check-only", 5, 60);
        quotaService.createOrUpdateQuota(quota);

        // When - Check multiple times without consuming
        for (int i = 0; i < 10; i++) {
            QuotaCheckResponse response = quotaService.checkQuota("check-only", 1);
            assertTrue(response.isAllowed(), "Check-only should always be allowed for unused quota");
            assertEquals(0, response.getCurrentUsage(), "Usage should remain 0");
        }

        // Consume some quota
        quotaService.checkAndConsumeQuota("check-only", 3);

        // Check again
        QuotaCheckResponse response = quotaService.checkQuota("check-only", 2);
        assertTrue(response.isAllowed());
        assertEquals(3, response.getCurrentUsage());

        // Check with amount that would exceed quota
        response = quotaService.checkQuota("check-only", 3);
        assertFalse(response.isAllowed());
        assertEquals(3, response.getCurrentUsage());
    }
}
