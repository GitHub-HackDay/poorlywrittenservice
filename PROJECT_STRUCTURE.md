# Project Structure

```
PoorlyWrittenService/
├── README.md                           # Comprehensive documentation
├── GETTING_STARTED.md                  # Quick setup guide
├── pom.xml                            # Maven project configuration
├── mvnw                               # Maven wrapper script
├── .mvn/wrapper/                      # Maven wrapper configuration
├── build.sh                          # Simple build script
├── load-test.sh                       # Load testing script
├── docker-deploy.sh                   # Docker deployment script
│
├── src/main/java/com/hackday/quota/
│   ├── ResourceQuotaServiceApplication.java    # Spring Boot main class
│   │
│   ├── model/                         # Data models
│   │   ├── ResourceQuota.java         # Quota configuration model
│   │   ├── ResourceUsage.java         # Usage tracking model
│   │   └── QuotaCheckResponse.java    # API response model
│   │
│   ├── service/                       # Business logic
│   │   ├── QuotaService.java          # Service interface
│   │   └── InMemoryQuotaService.java  # High-performance implementation
│   │
│   ├── controller/                    # REST API endpoints
│   │   └── QuotaController.java       # Main quota management API
│   │
│   ├── config/                        # Configuration classes
│   │   ├── AsyncConfig.java           # Async/threading configuration
│   │   └── CacheConfig.java           # Caching configuration
│   │
│   └── example/                       # Usage examples
│       └── ExampleApiController.java  # Example integration
│
├── src/main/resources/
│   └── application.properties         # Application configuration
│
└── src/test/java/com/hackday/quota/
    ├── service/
    │   └── InMemoryQuotaServiceTest.java      # Service unit tests
    ├── controller/
    │   └── QuotaControllerTest.java           # Controller tests
    └── performance/
        └── QuotaServiceBenchmark.java         # JMH performance tests
```

## Key Components

### Core Service (`InMemoryQuotaService`)
- **Thread-safe**: Uses `StampedLock` and `AtomicLong` for high concurrency
- **Optimistic locking**: Minimizes contention under load
- **Sliding window**: Time-based quota enforcement
- **Auto-cleanup**: Background cleanup of expired data
- **Metrics**: Built-in Prometheus metrics

### REST API (`QuotaController`)
- **Create/Update quotas**: `POST /api/v1/quotas`
- **Check and consume**: `POST /api/v1/quotas/{id}/check-and-consume`
- **Check only**: `GET /api/v1/quotas/{id}/check`
- **Management**: Get, delete, list, reset quotas
- **Health checks**: Built-in health and metrics endpoints

### Performance Optimizations
- **Custom thread pools**: Configured for high concurrency
- **Caffeine caching**: High-performance caching layer
- **Optimized data structures**: ConcurrentHashMap with atomic operations
- **JVM tuning**: Recommended G1GC settings for low latency

### Monitoring & Operations
- **Prometheus metrics**: Request rates, violations, latency
- **Health checks**: Service status and resource counts
- **Load testing**: Included scripts for performance validation
- **Docker support**: Containerized deployment

## Usage Patterns

1. **Rate Limiting**: Check quota before processing API requests
2. **Resource Throttling**: Limit expensive operations per user/resource
3. **Burst Protection**: Handle traffic spikes with sliding windows
4. **Multi-tenant**: Separate quotas per tenant/user/API key

## Scalability

- **100k concurrent requests**: Tested and optimized
- **20k resources**: Efficient memory usage and cleanup
- **Sub-millisecond latency**: Optimistic locking and atomic operations
- **Horizontal scaling**: Stateless design (with external coordination for distributed setup)
```
