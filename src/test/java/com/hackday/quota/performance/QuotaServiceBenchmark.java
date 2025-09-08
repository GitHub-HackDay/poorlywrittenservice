package com.hackday.quota.performance;

import com.hackday.quota.model.ResourceQuota;
import com.hackday.quota.service.InMemoryQuotaService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH Performance benchmarks for quota service
 * Run with: mvn test-compile exec:java -Dexec.mainClass="com.hackday.quota.performance.QuotaServiceBenchmark"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class QuotaServiceBenchmark {

    private InMemoryQuotaService quotaService;
    private static final String TEST_RESOURCE = "benchmark-resource";

    @Setup
    public void setup() {
        quotaService = new InMemoryQuotaService(new SimpleMeterRegistry());
        
        // Create a quota for testing
        ResourceQuota quota = new ResourceQuota(TEST_RESOURCE, 1000000, 3600); // High limit for benchmark
        quotaService.createOrUpdateQuota(quota);
    }

    @Benchmark
    @Threads(1)
    public void singleThreadQuotaCheck() {
        quotaService.checkAndConsumeQuota(TEST_RESOURCE, 1);
    }

    @Benchmark
    @Threads(10)
    public void tenThreadsQuotaCheck() {
        quotaService.checkAndConsumeQuota(TEST_RESOURCE, 1);
    }

    @Benchmark
    @Threads(50)
    public void fiftyThreadsQuotaCheck() {
        quotaService.checkAndConsumeQuota(TEST_RESOURCE, 1);
    }

    @Benchmark
    @Threads(100)
    public void hundredThreadsQuotaCheck() {
        quotaService.checkAndConsumeQuota(TEST_RESOURCE, 1);
    }

    @Benchmark
    @Threads(1)
    public void quotaCheckWithoutConsume() {
        quotaService.checkQuota(TEST_RESOURCE, 1);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(QuotaServiceBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
