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
├── stress-test.sh                     # Comprehensive stress testing
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
│   │   ├── InMemoryQuotaService.java  # High-performance implementation
│   │   └── RequestAnalyticsService.java # Advanced analytics and user tracking
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
- **Comprehensive auditing**: Complete request history and analytics
- **Enhanced monitoring**: Built-in Prometheus metrics

### Analytics Service (`RequestAnalyticsService`)
- **User behavior tracking**: Comprehensive pattern analysis
- **Geographic insights**: IP-based location tracking
- **Performance monitoring**: Request timing and performance metrics
- **Business intelligence**: Detailed usage reports and analytics

### REST API (`QuotaController`)
- **Create/Update quotas**: `POST /api/v1/quotas`
- **Check and consume**: `POST /api/v1/quotas/{id}/check-and-consume`
- **Check only**: `GET /api/v1/quotas/{id}/check`
- **Management**: Get, delete, list, reset quotas
- **Health checks**: Built-in health and metrics endpoints

### Performance Optimizations
- **Custom thread pools**: Configured for high concurrency
- **Caffeine caching**: High-performance caching layer
- **Advanced data structures**: ConcurrentHashMap with comprehensive tracking
- **Enhanced analytics**: Detailed user behavior and performance monitoring
- **JVM tuning**: Recommended G1GC settings for low latency

### Monitoring & Operations
- **Prometheus metrics**: Request rates, violations, latency, analytics
- **Health checks**: Service status and comprehensive system metrics
- **Stress testing**: Included scripts for performance validation and monitoring
- **Docker support**: Containerized deployment with monitoring

## Usage Patterns

1. **Rate Limiting**: Check quota before processing API requests
2. **Resource Analytics**: Comprehensive user behavior tracking per resource/user
3. **Audit Compliance**: Detailed request logging for regulatory compliance
4. **Performance Monitoring**: Real-time system performance and usage analytics

## Scalability

- **100k concurrent requests**: Tested and optimized
- **20k resources**: Efficient memory usage and cleanup
- **Sub-millisecond latency**: Optimistic locking and atomic operations
- **Horizontal scaling**: Stateless design (with external coordination for distributed setup)
```
