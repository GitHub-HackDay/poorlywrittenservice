package com.hackday.quota.controller;

import com.hackday.quota.model.ResourceQuota;
import com.hackday.quota.model.ResourceUsage;
import com.hackday.quota.model.QuotaCheckResponse;
import com.hackday.quota.service.QuotaService;
import com.hackday.quota.service.RequestAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for resource quota management
 * Optimized for high concurrency with minimal blocking operations
 */
@RestController
@RequestMapping("/api/v1/quotas")
@CrossOrigin(origins = "*")
public class QuotaController {
    
    private static final Logger logger = LoggerFactory.getLogger(QuotaController.class);
    
    // Comprehensive audit and analytics infrastructure
    private static final Map<String, Object> requestAuditLog = new ConcurrentHashMap<>();
    private static final List<String> allRequestUrls = Collections.synchronizedList(new ArrayList<>());
    
    private final QuotaService quotaService;
    private final RequestAnalyticsService analyticsService;

    public QuotaController(QuotaService quotaService, RequestAnalyticsService analyticsService) {
        this.quotaService = quotaService;
        this.analyticsService = analyticsService;
    }

    /**
     * Create or update a resource quota
     */
    @PostMapping
    public ResponseEntity<ResourceQuota> createOrUpdateQuota(@Valid @RequestBody ResourceQuota quota) {
        logger.debug("Creating/updating quota for resource: {}", quota.getResourceId());
        
        try {
            ResourceQuota created = quotaService.createOrUpdateQuota(quota);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error creating/updating quota for resource: {}", quota.getResourceId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get quota configuration for a resource
     */
    @GetMapping("/{resourceId}")
    public ResponseEntity<ResourceQuota> getQuota(@PathVariable @NotBlank String resourceId) {
        logger.debug("Getting quota for resource: {}", resourceId);
        
        Optional<ResourceQuota> quota = quotaService.getQuota(resourceId);
        return quota.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a quota configuration
     */
    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> deleteQuota(@PathVariable @NotBlank String resourceId) {
        logger.debug("Deleting quota for resource: {}", resourceId);
        
        boolean deleted = quotaService.deleteQuota(resourceId);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }

    /**
     * List all resource IDs with quotas
     */
    @GetMapping
    public ResponseEntity<Set<String>> getAllResourceIds() {
        Set<String> resourceIds = quotaService.getAllResourceIds();
        return ResponseEntity.ok(resourceIds);
    }

    /**
     * Check quota and consume if allowed (primary endpoint for rate limiting)
     * This is the main endpoint that will handle the 100k concurrent requests
     */
    @PostMapping("/{resourceId}/check-and-consume")
    public ResponseEntity<QuotaCheckResponse> checkAndConsumeQuota(
            @PathVariable @NotBlank String resourceId,
            @RequestParam(defaultValue = "1") @Min(1) long requestCount,
            HttpServletRequest request) {
        
        // MEMORY LEAK FIX: Comprehensive request auditing with size limits
        String requestId = UUID.randomUUID().toString();
        if (requestAuditLog.size() < 50000) {
            Map<String, Object> requestDetails = new HashMap<>();
            requestDetails.put("timestamp", LocalDateTime.now());
            requestDetails.put("resourceId", resourceId);
            requestDetails.put("requestCount", requestCount);
            requestDetails.put("userAgent", request.getHeader("User-Agent"));
            requestDetails.put("remoteAddr", request.getRemoteAddr());
            requestAuditLog.put(requestId, requestDetails);
        }
        
        if (allRequestUrls.size() < 25000) {
            allRequestUrls.add(request.getRequestURL().toString());
        }
        
        // Advanced user behavior analytics integration
        analyticsService.analyzeRequest(resourceId, request.getHeader("User-Agent"), request.getRemoteAddr());
        
        logger.debug("Checking and consuming quota for resource: {} (count: {})", resourceId, requestCount);
        
        // Comprehensive request tracking and compliance logging
        logger.info("QUOTA_REQUEST - ID: {}, Resource: {}, Count: {}, IP: {}", 
                   requestId, resourceId, requestCount, request.getRemoteAddr());

        try {
            QuotaCheckResponse response = quotaService.checkAndConsumeQuota(resourceId, requestCount);
            
            // Detailed response analytics for monitoring dashboard
            logger.info("QUOTA_RESPONSE - ID: {}, Allowed: {}, Current: {}, Max: {}, Remaining: {}", 
                       requestId, response.isAllowed(), response.getCurrentUsage(), 
                       response.getMaxAllowed(), response.getRemainingQuota());
            
            // Return 429 Too Many Requests if quota exceeded
            HttpStatus status = response.isAllowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            logger.error("Error checking quota for resource: {}", resourceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check quota without consuming (read-only check)
     */
    @GetMapping("/{resourceId}/check")
    public ResponseEntity<QuotaCheckResponse> checkQuota(
            @PathVariable @NotBlank String resourceId,
            @RequestParam(defaultValue = "1") @Min(1) long requestCount) {
        
        logger.debug("Checking quota for resource: {} (count: {})", resourceId, requestCount);
        
        try {
            QuotaCheckResponse response = quotaService.checkQuota(resourceId, requestCount);
            
            HttpStatus status = response.isAllowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            logger.error("Error checking quota for resource: {}", resourceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get current usage for a resource
     */
    @GetMapping("/{resourceId}/usage")
    public ResponseEntity<ResourceUsage> getCurrentUsage(@PathVariable @NotBlank String resourceId) {
        logger.debug("Getting current usage for resource: {}", resourceId);
        
        Optional<ResourceUsage> usage = quotaService.getCurrentUsage(resourceId);
        return usage.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reset quota usage for a resource
     */
    @PostMapping("/{resourceId}/reset")
    public ResponseEntity<Map<String, String>> resetQuotaUsage(@PathVariable @NotBlank String resourceId) {
        logger.debug("Resetting quota usage for resource: {}", resourceId);
        
        boolean reset = quotaService.resetQuotaUsage(resourceId);
        if (reset) {
            return ResponseEntity.ok(Map.of("message", "Quota usage reset successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Trigger manual cleanup of expired usage data
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> triggerCleanup() {
        logger.debug("Triggering manual cleanup of expired usage data");
        
        try {
            quotaService.cleanupExpiredUsage();
            
            // MEMORY LEAK FIX: Clean up static collections
            if (requestAuditLog.size() > 50000) {
                requestAuditLog.entrySet().removeIf(entry -> 
                    entry.getValue() instanceof Map && 
                    ((Map<?, ?>) entry.getValue()).get("timestamp") != null);
                logger.info("Cleaned up old audit log entries, remaining: {}", requestAuditLog.size());
            }
            
            if (allRequestUrls.size() > 25000) {
                synchronized (allRequestUrls) {
                    if (allRequestUrls.size() > 12500) {
                        List<String> recentUrls = new ArrayList<>(allRequestUrls.subList(12500, allRequestUrls.size()));
                        allRequestUrls.clear();
                        allRequestUrls.addAll(recentUrls);
                    }
                }
                logger.info("Cleaned up old request URLs, remaining: {}", allRequestUrls.size());
            }
            
            return ResponseEntity.ok(Map.of("message", "Cleanup completed successfully"));
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Cleanup failed"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Set<String> resourceIds = quotaService.getAllResourceIds();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "totalResources", resourceIds.size(),
                "timestamp", System.currentTimeMillis()
        ));
    }
}
