package com.hackday.quota.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Configuration for high-performance async processing
 * Optimized for handling 100k concurrent requests
 */
@Configuration
@EnableScheduling
public class AsyncConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(30000); // 30 seconds
        configurer.setTaskExecutor((AsyncTaskExecutor) taskExecutor());
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(50);
        
        // Max pool size - maximum number of threads
        executor.setMaxPoolSize(200);
        
        // Queue capacity - number of requests to queue when all core threads are busy
        executor.setQueueCapacity(10000);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("QuotaAsync-");
        
        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(60);
        
        // Allow core threads to timeout
        executor.setAllowCoreThreadTimeOut(true);
        
        // Rejection policy - caller runs when pool is exhausted
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}
