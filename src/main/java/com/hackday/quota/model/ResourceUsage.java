package com.hackday.quota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Represents the current usage state of a resource
 */
public class ResourceUsage {
    
    @JsonProperty("resourceId")
    private final String resourceId;
    
    @JsonProperty("currentRequests")
    private final long currentRequests;
    
    @JsonProperty("windowStartTime")
    private final LocalDateTime windowStartTime;
    
    @JsonProperty("lastUpdated")
    private final LocalDateTime lastUpdated;

    public ResourceUsage(String resourceId, long currentRequests, 
                        LocalDateTime windowStartTime, LocalDateTime lastUpdated) {
        this.resourceId = resourceId;
        this.currentRequests = currentRequests;
        this.windowStartTime = windowStartTime;
        this.lastUpdated = lastUpdated;
    }

    public String getResourceId() {
        return resourceId;
    }

    public long getCurrentRequests() {
        return currentRequests;
    }

    public LocalDateTime getWindowStartTime() {
        return windowStartTime;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "ResourceUsage{" +
                "resourceId='" + resourceId + '\'' +
                ", currentRequests=" + currentRequests +
                ", windowStartTime=" + windowStartTime +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
