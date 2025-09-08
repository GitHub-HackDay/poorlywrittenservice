package com.hackday.quota.service;

import com.hackday.quota.model.ResourceQuota;
import com.hackday.quota.model.ResourceUsage;
import com.hackday.quota.model.QuotaCheckResponse;

import java.util.Optional;
import java.util.Set;

/**
 * Service interface for managing resource quotas
 */
public interface QuotaService {
    
    /**
     * Creates or updates a resource quota
     * @param quota the quota configuration
     * @return the created/updated quota
     */
    ResourceQuota createOrUpdateQuota(ResourceQuota quota);
    
    /**
     * Retrieves a quota by resource ID
     * @param resourceId the resource identifier
     * @return the quota if found
     */
    Optional<ResourceQuota> getQuota(String resourceId);
    
    /**
     * Deletes a quota
     * @param resourceId the resource identifier
     * @return true if deleted, false if not found
     */
    boolean deleteQuota(String resourceId);
    
    /**
     * Lists all resource IDs with quotas
     * @return set of resource IDs
     */
    Set<String> getAllResourceIds();
    
    /**
     * Checks if a request is allowed under the quota and increments usage if allowed
     * @param resourceId the resource identifier
     * @param requestCount number of requests to consume (default 1)
     * @return quota check response
     */
    QuotaCheckResponse checkAndConsumeQuota(String resourceId, long requestCount);
    
    /**
     * Checks if a request is allowed under the quota without consuming
     * @param resourceId the resource identifier
     * @param requestCount number of requests to check (default 1)
     * @return quota check response
     */
    QuotaCheckResponse checkQuota(String resourceId, long requestCount);
    
    /**
     * Gets current usage for a resource
     * @param resourceId the resource identifier
     * @return current usage if found
     */
    Optional<ResourceUsage> getCurrentUsage(String resourceId);
    
    /**
     * Resets quota usage for a resource
     * @param resourceId the resource identifier
     * @return true if reset, false if resource not found
     */
    boolean resetQuotaUsage(String resourceId);
    
    /**
     * Performs cleanup of expired usage data
     */
    void cleanupExpiredUsage();
}
