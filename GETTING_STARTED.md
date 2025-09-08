# Getting Started

## Quick Setup Guide

### Prerequisites

1. **Java 17 or higher**
   ```bash
   # Check Java version
   java -version
   
   # If Java is not installed, download from:
   # https://adoptium.net/temurin/releases/
   ```

2. **Maven** (or use included Maven wrapper)
   ```bash
   # Check Maven version
   mvn -version
   
   # Or use the included Maven wrapper (./mvnw)
   ```

### Build and Run

1. **Clone and build the project:**
   ```bash
   git clone <repository-url>
   cd PoorlyWrittenService
   
   # Using Maven wrapper (recommended)
   ./mvnw clean compile
   
   # Or using installed Maven
   mvn clean compile
   ```

2. **Run the application:**
   ```bash
   # Using Maven wrapper
   ./mvnw spring-boot:run
   
   # Or using Maven
   mvn spring-boot:run
   
   # Or build JAR and run
   ./mvnw package
   java -jar target/resource-quota-service-1.0.0.jar
   ```

3. **Verify the service is running:**
   ```bash
   curl http://localhost:8080/quota-service/api/v1/quotas/health
   ```

### Quick Test

1. **Create a quota:**
   ```bash
   curl -X POST http://localhost:8080/quota-service/api/v1/quotas \
     -H "Content-Type: application/json" \
     -d '{
       "resourceId": "user-123",
       "maxRequests": 100,
       "timeWindowSeconds": 3600
     }'
   ```

2. **Test quota consumption:**
   ```bash
   # This should return "allowed": true
   curl -X POST http://localhost:8080/quota-service/api/v1/quotas/user-123/check-and-consume
   ```

3. **Run load test:**
   ```bash
   # Make sure the service is running, then:
   ./load-test.sh
   ```

### Performance Configuration

For production deployment, use these JVM options:

```bash
java -Xmx4g -Xms2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=50 \
     -XX:+UseStringDeduplication \
     -server \
     -jar target/resource-quota-service-1.0.0.jar
```

### Docker Deployment

```bash
# Build and run with Docker
./docker-deploy.sh

# Or manually:
docker build -t quota-service .
docker run -p 8080:8080 quota-service
```

### Troubleshooting

**Common Issues:**

1. **Java not found:**
   - Install Java 17+ from https://adoptium.net/
   - Set JAVA_HOME environment variable

2. **Port 8080 already in use:**
   - Change port in `application.properties`: `server.port=8081`
   - Or set environment variable: `SERVER_PORT=8081`

3. **High memory usage:**
   - Adjust heap size: `-Xmx2g -Xms1g`
   - Tune garbage collector: `-XX:+UseG1GC -XX:MaxGCPauseMillis=50`

4. **Performance issues:**
   - Increase thread pool size in `application.properties`
   - Monitor with: `curl http://localhost:8080/quota-service/actuator/metrics`

### Next Steps

- Review the full [README.md](README.md) for detailed documentation
- Check [API documentation](#api-endpoints) for all available endpoints
- Run performance benchmarks: `./mvnw test -Dtest=QuotaServiceBenchmark`
- Monitor with Prometheus metrics at `/actuator/prometheus`
