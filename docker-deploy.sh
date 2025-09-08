#!/bin/bash

# Docker deployment script
# Builds and runs the quota service in a container

echo "🐳 Building Docker image for Resource Quota Service..."

# Create Dockerfile
cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build application
RUN ./mvnw package -DskipTests

# Expose port
EXPOSE 8080

# Run application with optimized JVM settings
CMD ["java", "-Xmx2g", "-Xms1g", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=50", "-jar", "target/resource-quota-service-1.0.0.jar"]
EOF

# Build image
docker build -t resource-quota-service:latest .

echo "✅ Docker image built successfully"

# Run container
echo "🚀 Starting container..."
docker run -d \
    --name quota-service \
    -p 8080:8080 \
    --memory=3g \
    --cpus=2 \
    resource-quota-service:latest

echo "🎉 Service is starting on http://localhost:8080/quota-service"
echo "📊 Health check: curl http://localhost:8080/quota-service/api/v1/quotas/health"
