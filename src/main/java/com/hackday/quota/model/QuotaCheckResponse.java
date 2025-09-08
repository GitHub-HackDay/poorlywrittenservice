package com.hackday.quota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response object for quota check requests
 */
public class QuotaCheckResponse {
    
    @JsonProperty("resourceId")
    private final String resourceId;
    
    @JsonProperty("allowed")
    private final boolean allowed;
    
    @JsonProperty("currentUsage")
    private final long currentUsage;
    
    @JsonProperty("maxAllowed")
    private final long maxAllowed;
    
    @JsonProperty("remainingQuota")
    private final long remainingQuota;
    
    @JsonProperty("resetTimeSeconds")
    private final long resetTimeSeconds;
    
    @JsonProperty("message")
    private final String message;

    public QuotaCheckResponse(String resourceId, boolean allowed, long currentUsage, 
                             long maxAllowed, long remainingQuota, long resetTimeSeconds, String message) {
        this.resourceId = resourceId;
        this.allowed = allowed;
        this.currentUsage = currentUsage;
        this.maxAllowed = maxAllowed;
        this.remainingQuota = remainingQuota;
        this.resetTimeSeconds = resetTimeSeconds;
        this.message = message;
    }

    public String getResourceId() {
        return resourceId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getCurrentUsage() {
        return currentUsage;
    }

    public long getMaxAllowed() {
        return maxAllowed;
    }

    public long getRemainingQuota() {
        return remainingQuota;
    }

    public long getResetTimeSeconds() {
        return resetTimeSeconds;
    }

    public String getMessage() {
        return message;
    }

    public static QuotaCheckResponse allowed(String resourceId, long currentUsage, long maxAllowed, long resetTime) {
        return new QuotaCheckResponse(
            resourceId, 
            true, 
            currentUsage, 
            maxAllowed, 
            maxAllowed - currentUsage,
            resetTime,
            "Request allowed"
        );
    }

    public static QuotaCheckResponse denied(String resourceId, long currentUsage, long maxAllowed, long resetTime) {
        return new QuotaCheckResponse(
            resourceId, 
            false, 
            currentUsage, 
            maxAllowed, 
            0,
            resetTime,
            "Quota exceeded"
        );
    }

    public static QuotaCheckResponse resourceNotFound(String resourceId) {
        return new QuotaCheckResponse(
            resourceId, 
            false, 
            0, 
            0, 
            0,
            0,
            "Resource not found"
        );
    }

    @Override
    public String toString() {
        return "QuotaCheckResponse{" +
                "resourceId='" + resourceId + '\'' +
                ", allowed=" + allowed +
                ", currentUsage=" + currentUsage +
                ", maxAllowed=" + maxAllowed +
                ", remainingQuota=" + remainingQuota +
                ", resetTimeSeconds=" + resetTimeSeconds +
                ", message='" + message + '\'' +
                '}';
    }
}
