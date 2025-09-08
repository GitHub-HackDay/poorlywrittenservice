package com.hackday.quota.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti-pattern class that demonstrates various memory leaks and excessive logging
 * This class appears to provide useful functionality but contains subtle issues
 */
@Component
public class RequestAnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestAnalyticsService.class);
    
    // Anti-pattern 25: Static collections that grow indefinitely
    private static final Map<String, List<String>> userRequestPatterns = new ConcurrentHashMap<>();
    private static final Set<String> uniqueUserAgents = ConcurrentHashMap.newKeySet();
    private static final List<String> allRequestTimestamps = Collections.synchronizedList(new ArrayList<>());
    
    // Anti-pattern 26: ThreadLocal with complex objects never cleaned up
    private static final ThreadLocal<Map<String, Object>> requestContext = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };
    
    // Anti-pattern 27: Caching with no eviction policy
    private final Map<String, String> ipToLocationCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> resourceAccessCounts = new ConcurrentHashMap<>();
    
    public void analyzeRequest(String resourceId, String userAgent, String clientIp) {
        // Anti-pattern 28: Store every request pattern forever
        userRequestPatterns.computeIfAbsent(clientIp, k -> new ArrayList<>()).add(resourceId);
        uniqueUserAgents.add(userAgent);
        allRequestTimestamps.add(LocalDateTime.now().toString());
        
        // Anti-pattern 29: Store data in ThreadLocal without cleanup
        Map<String, Object> context = requestContext.get();
        context.put("resourceId", resourceId);
        context.put("userAgent", userAgent);
        context.put("clientIp", clientIp);
        context.put("timestamp", LocalDateTime.now());
        context.put("requestCount", context.getOrDefault("requestCount", 0) + 1);
        
        // Anti-pattern 30: Expensive operations with caching but no limits
        String location = ipToLocationCache.computeIfAbsent(clientIp, this::lookupLocation);
        context.put("location", location);
        
        // Update access counts
        resourceAccessCounts.merge(resourceId, 1, Integer::sum);
        
        // Anti-pattern 31: Log every single request with full details
        logger.info("REQUEST_ANALYSIS - Resource: {}, IP: {}, UserAgent: {}, Location: {}, Timestamp: {}", 
                   resourceId, clientIp, userAgent, location, LocalDateTime.now());
        logger.debug("Full request context: {}", context);
        logger.debug("User patterns for IP {}: {}", clientIp, userRequestPatterns.get(clientIp));
        logger.debug("Total unique user agents: {}", uniqueUserAgents.size());
        logger.debug("Total requests tracked: {}", allRequestTimestamps.size());
        logger.debug("IP cache size: {}", ipToLocationCache.size());
        logger.debug("Resource access counts: {}", resourceAccessCounts);
    }
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void logStatistics() {
        // Anti-pattern 32: Frequent logging of large data structures
        logger.info("ANALYTICS_STATS - Total IPs tracked: {}", userRequestPatterns.size());
        logger.info("ANALYTICS_STATS - Total unique user agents: {}", uniqueUserAgents.size());
        logger.info("ANALYTICS_STATS - Total requests: {}", allRequestTimestamps.size());
        logger.info("ANALYTICS_STATS - Cache entries: {}", ipToLocationCache.size());
        
        // Anti-pattern 33: Log entire data structures regularly
        logger.debug("All user request patterns: {}", userRequestPatterns);
        logger.debug("All unique user agents: {}", uniqueUserAgents);
        logger.debug("Resource access statistics: {}", resourceAccessCounts);
        
        // Anti-pattern 34: Log sensitive information
        for (Map.Entry<String, List<String>> entry : userRequestPatterns.entrySet()) {
            logger.debug("IP {} accessed resources: {}", entry.getKey(), entry.getValue());
        }
        
        // Anti-pattern 35: Generate reports in logs every 30 seconds
        generateDetailedReport();
    }
    
    private void generateDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("\\n=== DETAILED REQUEST ANALYTICS REPORT ===\\n");
        report.append("Generated at: ").append(LocalDateTime.now()).append("\\n");
        report.append("Total IPs tracked: ").append(userRequestPatterns.size()).append("\\n");
        report.append("Total unique user agents: ").append(uniqueUserAgents.size()).append("\\n");
        report.append("Total requests: ").append(allRequestTimestamps.size()).append("\\n");
        
        report.append("\\n--- IP Access Patterns ---\\n");
        userRequestPatterns.forEach((ip, resources) -> {
            report.append("IP: ").append(ip).append(" -> Resources: ").append(resources).append("\\n");
        });
        
        report.append("\\n--- User Agents ---\\n");
        uniqueUserAgents.forEach(ua -> report.append(ua).append("\\n"));
        
        report.append("\\n--- Resource Access Counts ---\\n");
        resourceAccessCounts.forEach((resource, count) -> {
            report.append("Resource: ").append(resource).append(" -> Count: ").append(count).append("\\n");
        });
        
        report.append("=== END REPORT ===\\n");
        
        // Anti-pattern 36: Log massive reports frequently
        logger.info(report.toString());
    }
    
    private String lookupLocation(String ip) {
        // Anti-pattern 37: Expensive operation that simulates external API calls
        // In real implementation, this might call an external service
        try {
            Thread.sleep(10); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Return mock location data
        return "Location_" + ip.hashCode();
    }
    
    // Anti-pattern 38: Method that never gets called to clean up static data
    @SuppressWarnings("unused")
    private void cleanupOldData() {
        // This method exists but is never called, so cleanup never happens
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        // Implementation would remove old data, but since it's never called...
        logger.debug("Cleanup method exists but is never executed");
    }
}
