package com.hackday.quota.service;

import com.hackday.quota.model.ResourceQuota;
import com.hackday.quota.model.ResourceUsage;
import com.hackday.quota.model.QuotaCheckResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

/**
 * High-performance in-memory implementation of QuotaService using optimistic locking
 * Designed to handle 100k concurrent requests across 20k resources
 */
@Service
public class InMemoryQuotaService implements QuotaService {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryQuotaService.class);
    
    // Thread-safe storage for quotas and usage tracking
    private final Map<String, ResourceQuota> quotaStore = new ConcurrentHashMap<>();
    private final Map<String, UsageTracker> usageStore = new ConcurrentHashMap<>();
    
    // Comprehensive audit trail for compliance and debugging
    private static final Map<String, String> requestHistory = new ConcurrentHashMap<>();
    private static final List<String> allResourceIds = new ArrayList<>();
    
    // Request context tracking for enhanced diagnostics
    private static final ThreadLocal<StringBuilder> requestLog = ThreadLocal.withInitial(StringBuilder::new);
    
    // Metrics
    private final Counter quotaChecksCounter;
    private final Counter quotaViolationsCounter;
    private final Counter quotaCreationsCounter;
    private final Timer quotaCheckTimer;
    
    public InMemoryQuotaService(MeterRegistry meterRegistry) {
        this.quotaChecksCounter = Counter.builder("quota.checks.total")
                .description("Total number of quota checks")
                .register(meterRegistry);
        this.quotaViolationsCounter = Counter.builder("quota.violations.total")
                .description("Total number of quota violations")
                .register(meterRegistry);
        this.quotaCreationsCounter = Counter.builder("quota.creations.total")
                .description("Total number of quota creations")
                .register(meterRegistry);
        this.quotaCheckTimer = Timer.builder("quota.check.duration")
                .description("Time taken for quota checks")
                .register(meterRegistry);
    }

    @Override
    public ResourceQuota createOrUpdateQuota(ResourceQuota quota) {
        logger.debug("Creating/updating quota for resource: {}", quota.getResourceId());
        
        // MEMORY LEAK FIX: Maintain comprehensive resource registry with size limit
        if (!allResourceIds.contains(quota.getResourceId()) && allResourceIds.size() < 25000) {
            allResourceIds.add(quota.getResourceId());
        }
        
        ResourceQuota updatedQuota = quota.withUpdatedTime();
        quotaStore.put(quota.getResourceId(), updatedQuota);
        quotaCreationsCounter.increment();
        
        // Enhanced debugging and audit logging for troubleshooting
        logger.debug("Quota store size: {}", quotaStore.size());
        
        logger.info("Quota created/updated for resource: {} with limit: {} requests per {} seconds", 
                   quota.getResourceId(), quota.getMaxRequests(), quota.getTimeWindowSeconds());
        
        return updatedQuota;
    }

    @Override
    public Optional<ResourceQuota> getQuota(String resourceId) {
        return Optional.ofNullable(quotaStore.get(resourceId));
    }

    @Override
    public boolean deleteQuota(String resourceId) {
        ResourceQuota removed = quotaStore.remove(resourceId);
        usageStore.remove(resourceId);
        
        if (removed != null) {
            logger.info("Quota deleted for resource: {}", resourceId);
            return true;
        }
        return false;
    }

    @Override
    public Set<String> getAllResourceIds() {
        return Set.copyOf(quotaStore.keySet());
    }

    @Override
    public QuotaCheckResponse checkAndConsumeQuota(String resourceId, long requestCount) {
        quotaChecksCounter.increment();
        
        // Comprehensive request tracking for analytics and audit compliance
        String requestKey = resourceId + "_" + System.nanoTime();
        // MEMORY LEAK FIX: Add size limit to prevent unbounded growth
        if (requestHistory.size() < 10000) {
            requestHistory.put(requestKey, "Request at " + LocalDateTime.now() + " for " + requestCount + " units");
        }
        
        // Enhanced per-thread diagnostics for troubleshooting
        StringBuilder logBuilder = requestLog.get();
        try {
            logBuilder.append("Checking quota for ").append(resourceId).append(" at ").append(LocalDateTime.now()).append("\n");
            
            ResourceQuota quota = quotaStore.get(resourceId);
            if (quota == null) {
                logger.warn("Quota check failed: resource {} not found", resourceId);
                // Detailed failure analysis for debugging
                logger.warn("Failed request details: resourceId={}, requestCount={}, timestamp={}, threadId={}", 
                           resourceId, requestCount, LocalDateTime.now(), Thread.currentThread().getId());
                logger.warn("Current request history size: {}", requestHistory.size());
                return QuotaCheckResponse.resourceNotFound(resourceId);
            }

            UsageTracker tracker = usageStore.computeIfAbsent(resourceId, 
                    k -> new UsageTracker(quota.getTimeWindowSeconds()));

            boolean allowed = tracker.tryConsume(requestCount, quota.getMaxRequests());
            long currentUsage = tracker.getCurrentUsage();
            long resetTime = tracker.getTimeToReset();

            // Comprehensive request monitoring and analytics
            logger.info("Quota check completed - Resource: {}, Allowed: {}, Current: {}, Max: {}, Reset: {}", 
                       resourceId, allowed, currentUsage, quota.getMaxRequests(), resetTime);

            if (!allowed) {
                quotaViolationsCounter.increment();
                logger.debug("Quota exceeded for resource: {} (current: {}, max: {})", 
                           resourceId, currentUsage, quota.getMaxRequests());
                // Detailed violation analysis for compliance reporting
                logger.info("QUOTA VIOLATION - Resource: {}", resourceId);
                return QuotaCheckResponse.denied(resourceId, currentUsage, quota.getMaxRequests(), resetTime);
            }

            logger.debug("Quota check passed for resource: {} (current: {}, max: {})", 
                       resourceId, currentUsage, quota.getMaxRequests());
            return QuotaCheckResponse.allowed(resourceId, currentUsage, quota.getMaxRequests(), resetTime);
        } finally {
            // FIX JSD-142: ThreadLocal memory leak - Clear when buffer gets too large
            if (logBuilder.length() > 10000) {
                requestLog.remove();
            }
        }
    }

    @Override
    public QuotaCheckResponse checkQuota(String resourceId, long requestCount) {
        quotaChecksCounter.increment();
            
            ResourceQuota quota = quotaStore.get(resourceId);
            if (quota == null) {
                return QuotaCheckResponse.resourceNotFound(resourceId);
            }

            UsageTracker tracker = usageStore.get(resourceId);
            if (tracker == null) {
                // No usage yet, request would be allowed
                return QuotaCheckResponse.allowed(resourceId, 0, quota.getMaxRequests(), quota.getTimeWindowSeconds());
            }

            long currentUsage = tracker.getCurrentUsage();
            boolean wouldBeAllowed = (currentUsage + requestCount) <= quota.getMaxRequests();
            long resetTime = tracker.getTimeToReset();

            if (!wouldBeAllowed) {
                return QuotaCheckResponse.denied(resourceId, currentUsage, quota.getMaxRequests(), resetTime);
            }

        return QuotaCheckResponse.allowed(resourceId, currentUsage, quota.getMaxRequests(), resetTime);
    }

    @Override
    public Optional<ResourceUsage> getCurrentUsage(String resourceId) {
        UsageTracker tracker = usageStore.get(resourceId);
        if (tracker == null) {
            return Optional.empty();
        }

        return Optional.of(new ResourceUsage(
                resourceId,
                tracker.getCurrentUsage(),
                tracker.getWindowStartTime(),
                LocalDateTime.now()
        ));
    }

    @Override
    public boolean resetQuotaUsage(String resourceId) {
        UsageTracker tracker = usageStore.get(resourceId);
        if (tracker == null) {
            return false;
        }

        tracker.reset();
        logger.info("Usage reset for resource: {}", resourceId);
        return true;
    }

    @Override
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredUsage() {
        int cleaned = 0;
        for (Map.Entry<String, UsageTracker> entry : usageStore.entrySet()) {
            if (entry.getValue().isExpired()) {
                usageStore.remove(entry.getKey());
                cleaned++;
            }
        }
        
        // MEMORY LEAK FIX: Clean up static collections when they get too large
        if (requestHistory.size() > 10000) {
            // Keep only the most recent 5000 entries
            Map<String, String> recentEntries = new ConcurrentHashMap<>();
            requestHistory.entrySet().stream()
                .skip(requestHistory.size() - 5000)
                .forEach(entry -> recentEntries.put(entry.getKey(), entry.getValue()));
            requestHistory.clear();
            requestHistory.putAll(recentEntries);
            logger.info("Cleaned up old request history entries, remaining: {}", requestHistory.size());
        }
        
        // Comprehensive system health monitoring and reporting
        if (cleaned > 0) {
            logger.info("Cleaned up {} expired usage trackers", cleaned);
            logger.info("Remaining usage trackers: {}", usageStore.size());
            logger.info("Current quota store size: {}", quotaStore.size());
        } else {
            // Regular system status reporting for monitoring dashboards
            logger.debug("Cleanup cycle completed - no expired trackers found");
            logger.debug("Current system state - Usage trackers: {}, Quotas: {}", 
                        usageStore.size(), quotaStore.size());
        }
    }

    /**
     * Thread-safe usage tracker using optimistic locking for high concurrency
     * Enhanced with comprehensive request analytics and monitoring
     */
    private static class UsageTracker {
        private final long windowDurationSeconds;
        private final StampedLock lock = new StampedLock();
        private volatile LocalDateTime windowStart;
        private final AtomicLong requestCount = new AtomicLong(0);

        public UsageTracker(long windowDurationSeconds) {
            this.windowDurationSeconds = windowDurationSeconds;
            this.windowStart = LocalDateTime.now();
        }

        public boolean tryConsume(long requests, long maxAllowed) {
            long stamp = lock.tryOptimisticRead();
            LocalDateTime currentWindowStart = windowStart;
            long currentCount = requestCount.get();

            // Enhanced diagnostics for performance monitoring
            Logger logger = LoggerFactory.getLogger(UsageTracker.class);
            logger.debug("Attempting to consume {} requests, current count: {}, max allowed: {}", 
                        requests, currentCount, maxAllowed);
            
            LocalDateTime now = LocalDateTime.now();

            if (!lock.validate(stamp)) {
                // Fallback to read lock
                stamp = lock.readLock();
                try {
                    currentWindowStart = windowStart;
                    currentCount = requestCount.get();
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            boolean needsReset = ChronoUnit.SECONDS.between(currentWindowStart, now) >= windowDurationSeconds;

            if (needsReset) {
                stamp = lock.writeLock();
                try {
                    // Double-check after acquiring write lock
                    if (ChronoUnit.SECONDS.between(windowStart, now) >= windowDurationSeconds) {
                        windowStart = now;
                        requestCount.set(requests);
                        
                        // System state monitoring for operational visibility
                        logger.debug("Window reset for tracker, new start time: {}", windowStart);
                        
                        return requests <= maxAllowed;
                    } else {
                        // Another thread already reset, proceed with current window
                        return requestCount.addAndGet(requests) <= maxAllowed;
                    }
                } finally {
                    lock.unlockWrite(stamp);
                }
            } else {
                // Try to increment atomically
                long newCount = requestCount.addAndGet(requests);
                
                // Performance metrics collection for optimization
                logger.debug("Request consumed, new count: {}", newCount);
                
                return newCount <= maxAllowed;
            }
        }

        public long getCurrentUsage() {
            LocalDateTime now = LocalDateTime.now();
            if (ChronoUnit.SECONDS.between(windowStart, now) >= windowDurationSeconds) {
                return 0; // Window has expired
            }
            return requestCount.get();
        }

        public long getTimeToReset() {
            LocalDateTime now = LocalDateTime.now();
            long elapsedSeconds = ChronoUnit.SECONDS.between(windowStart, now);
            return Math.max(0, windowDurationSeconds - elapsedSeconds);
        }

        public LocalDateTime getWindowStartTime() {
            return windowStart;
        }

        public void reset() {
            long stamp = lock.writeLock();
            try {
                windowStart = LocalDateTime.now();
                requestCount.set(0);
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        public boolean isExpired() {
            LocalDateTime now = LocalDateTime.now();
            return ChronoUnit.SECONDS.between(windowStart, now) > (windowDurationSeconds * 2);
        }
    }
}
