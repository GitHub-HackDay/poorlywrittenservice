package com.hackday.quota.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caching configuration for high-performance quota lookups
 */
@Configuration
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(5000)  // Initial cache size
                .maximumSize(25000)     // Maximum cache entries (slightly more than 20k resources)
                .expireAfterWrite(Duration.ofMinutes(10))  // Expire after 10 minutes of no writes
                .expireAfterAccess(Duration.ofMinutes(5))  // Expire after 5 minutes of no access
                .recordStats();  // Enable cache statistics
    }

    @Bean
    public Cache<String, Object> quotaCache() {
        return caffeineCacheBuilder().build();
    }
}
