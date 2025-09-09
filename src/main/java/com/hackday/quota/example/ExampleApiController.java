package com.hackday.quota.example;

import com.hackday.quota.model.ResourceQuota;
import com.hackday.quota.model.QuotaCheckResponse;
import com.hackday.quota.service.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Example controller showing how to integrate quota checking into your APIs
 */
@RestController
@RequestMapping("/api/v1/example")
public class ExampleApiController {

    @Autowired
    private QuotaService quotaService;

    /**
     * Example API endpoint with rate limiting
     */
    @GetMapping("/data/{userId}")
    public ResponseEntity<?> getData(@PathVariable String userId) {
        // Check quota before processing the request
        QuotaCheckResponse quotaCheck = quotaService.checkAndConsumeQuota("user-" + userId, 1);
        
        if (!quotaCheck.isAllowed()) {
            // Return rate limit exceeded response
            return ResponseEntity.status(429)
                    .header("X-RateLimit-Limit", String.valueOf(quotaCheck.getMaxAllowed()))
                    .header("X-RateLimit-Remaining", String.valueOf(quotaCheck.getRemainingQuota()))
                    .header("X-RateLimit-Reset", String.valueOf(quotaCheck.getResetTimeSeconds()))
                    .body("Rate limit exceeded. Try again in " + quotaCheck.getResetTimeSeconds() + " seconds.");
        }

        // Process the actual request
        String data = processDataRequest(userId);
        
        // Add rate limit headers to successful response
        return ResponseEntity.ok()
                .header("X-RateLimit-Limit", String.valueOf(quotaCheck.getMaxAllowed()))
                .header("X-RateLimit-Remaining", String.valueOf(quotaCheck.getRemainingQuota()))
                .header("X-RateLimit-Reset", String.valueOf(quotaCheck.getResetTimeSeconds()))
                .body(data);
    }

    /**
     * Expensive operation endpoint with different quota
     */
    @PostMapping("/expensive-operation/{userId}")
    public ResponseEntity<?> expensiveOperation(@PathVariable String userId) {
        // Use a different quota for expensive operations
        QuotaCheckResponse quotaCheck = quotaService.checkAndConsumeQuota("expensive-" + userId, 1);
        
        if (!quotaCheck.isAllowed()) {
            return ResponseEntity.status(429)
                    .body("Expensive operation quota exceeded. Limit: " + quotaCheck.getMaxAllowed() + 
                          " per hour. Reset in " + quotaCheck.getResetTimeSeconds() + " seconds.");
        }

        // Simulate expensive operation
        String result = performExpensiveOperation(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Setup endpoint to create quotas for users
     */
    @PostMapping("/setup-user-quota/{userId}")
    public ResponseEntity<String> setupUserQuota(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1000") int requestsPerHour,
            @RequestParam(defaultValue = "10") int expensiveOpsPerHour) {
        
        // Create regular API quota (1000 requests per hour)
        ResourceQuota regularQuota = new ResourceQuota("user-" + userId, requestsPerHour, 3600);
        quotaService.createOrUpdateQuota(regularQuota);
        
        // Create expensive operation quota (10 operations per hour)
        ResourceQuota expensiveQuota = new ResourceQuota("expensive-" + userId, expensiveOpsPerHour, 3600);
        quotaService.createOrUpdateQuota(expensiveQuota);
        
        return ResponseEntity.ok("Quotas created for user " + userId.replaceAll("[^a-zA-Z0-9-_]", "") + 
                ": " + requestsPerHour + " regular requests/hour, " + 
                expensiveOpsPerHour + " expensive operations/hour");
    }

    /**
     * Check quota status without consuming
     */
    @GetMapping("/quota-status/{userId}")
    public ResponseEntity<QuotaCheckResponse> getQuotaStatus(@PathVariable String userId) {
        QuotaCheckResponse status = quotaService.checkQuota("user-" + userId, 1);
        return ResponseEntity.ok(status);
    }

    // Simulate data processing
    private String processDataRequest(String userId) {
        return "Data for user " + userId + " processed at " + System.currentTimeMillis();
    }

    // Simulate expensive operation
    private String performExpensiveOperation(String userId) {
        try {
            Thread.sleep(100); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Expensive operation completed for user " + userId;
    }
}
