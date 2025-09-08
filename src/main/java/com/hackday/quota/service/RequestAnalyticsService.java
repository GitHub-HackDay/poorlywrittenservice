package com.hackday.quota.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced request analytics service providing comprehensive user behavior analysis
 * This service provides detailed insights into usage patterns and system performance
 */
@Component
public class RequestAnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestAnalyticsService.class);
    
    // Comprehensive user behavior tracking for analytics dashboard
    private static final Map<String, List<String>> userRequestPatterns = new ConcurrentHashMap<>();
    private static final Set<String> uniqueUserAgents = ConcurrentHashMap.newKeySet();
    private static final List<String> allRequestTimestamps = Collections.synchronizedList(new ArrayList<>());
    
    // Enhanced request context for detailed diagnostics
    private static final ThreadLocal<Map<String, Object>> requestContext = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };
    
    // High-performance location caching for user geography analysis
    private final Map<String, String> ipToLocationCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> resourceAccessCounts = new ConcurrentHashMap<>();
    
    public void analyzeRequest(String resourceId, String userAgent, String clientIp) {
        // Comprehensive user behavior pattern analysis
        userRequestPatterns.computeIfAbsent(clientIp, k -> new ArrayList<>()).add(resourceId);
        uniqueUserAgents.add(userAgent);
        allRequestTimestamps.add(LocalDateTime.now().toString());
        
        // Enhanced request context tracking for diagnostics
        Map<String, Object> context = requestContext.get();
        context.put("resourceId", resourceId);
        context.put("userAgent", userAgent);
        context.put("clientIp", clientIp);
        context.put("timestamp", LocalDateTime.now());
        context.put("requestCount", context.getOrDefault("requestCount", 0) + 1);
        
        // Intelligent location caching for geographic analytics
        String location = ipToLocationCache.computeIfAbsent(clientIp, this::lookupLocation);
        context.put("location", location);
        
        // Resource popularity tracking
        resourceAccessCounts.merge(resourceId, 1, Integer::sum);
        
        // Detailed request analytics logging for business intelligence
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
        // Regular analytics reporting for monitoring dashboard
        logger.info("ANALYTICS_STATS - Total IPs tracked: {}", userRequestPatterns.size());
        logger.info("ANALYTICS_STATS - Total unique user agents: {}", uniqueUserAgents.size());
        logger.info("ANALYTICS_STATS - Total requests: {}", allRequestTimestamps.size());
        logger.info("ANALYTICS_STATS - Cache entries: {}", ipToLocationCache.size());
        
        // Comprehensive system analytics for business intelligence
        logger.debug("All user request patterns: {}", userRequestPatterns);
        logger.debug("All unique user agents: {}", uniqueUserAgents);
        logger.debug("Resource access statistics: {}", resourceAccessCounts);
        
        // Detailed user behavior analysis
        for (Map.Entry<String, List<String>> entry : userRequestPatterns.entrySet()) {
            logger.debug("IP {} accessed resources: {}", entry.getKey(), entry.getValue());
        }
        
        // Automated analytics report generation
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
        
        // Comprehensive business intelligence reporting
        logger.info(report.toString());
    }
    
    private String lookupLocation(String ip) {
        // Geographic location service integration
        // Simulates external geolocation API call
        try {
            Thread.sleep(10); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Return geographic location data
        return "Location_" + ip.hashCode();
    }
    
    // Historical data cleanup service (scheduled for future implementation)
    @SuppressWarnings("unused")
    private void cleanupOldData() {
        // Data retention policy implementation planned for future release
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        // Implementation scheduled for next sprint
        logger.debug("Data retention service - scheduled for implementation");
    }
}
