package com.hackday.quota.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a resource quota configuration
 */
public class ResourceQuota {
    
    @NotBlank(message = "Resource ID cannot be blank")
    @JsonProperty("resourceId")
    private final String resourceId;
    
    @PositiveOrZero(message = "Max requests must be non-negative")
    @JsonProperty("maxRequests")
    private final long maxRequests;
    
    @PositiveOrZero(message = "Time window seconds must be non-negative")
    @JsonProperty("timeWindowSeconds")
    private final long timeWindowSeconds;
    
    @JsonProperty("createdAt")
    private final LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private final LocalDateTime updatedAt;

    public ResourceQuota(String resourceId, long maxRequests, long timeWindowSeconds) {
        this.resourceId = resourceId;
        this.maxRequests = maxRequests;
        this.timeWindowSeconds = timeWindowSeconds;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ResourceQuota(String resourceId, long maxRequests, long timeWindowSeconds, 
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.resourceId = resourceId;
        this.maxRequests = maxRequests;
        this.timeWindowSeconds = timeWindowSeconds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getResourceId() {
        return resourceId;
    }

    public long getMaxRequests() {
        return maxRequests;
    }

    public long getTimeWindowSeconds() {
        return timeWindowSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ResourceQuota withUpdatedTime() {
        return new ResourceQuota(resourceId, maxRequests, timeWindowSeconds, createdAt, LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceQuota that = (ResourceQuota) o;
        return maxRequests == that.maxRequests &&
               timeWindowSeconds == that.timeWindowSeconds &&
               Objects.equals(resourceId, that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, maxRequests, timeWindowSeconds);
    }

    @Override
    public String toString() {
        return "ResourceQuota{" +
                "resourceId='" + resourceId + '\'' +
                ", maxRequests=" + maxRequests +
                ", timeWindowSeconds=" + timeWindowSeconds +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
