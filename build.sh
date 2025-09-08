#!/bin/bash

# Simple build and run script for the Resource Quota Service

set -e

echo "🔨 Building Resource Quota Service..."

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17+ and try again."
    echo "   Download from: https://adoptium.net/temurin/releases/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "([0-9]+)' | grep -oP '[0-9]+' | head -1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17+ is required. Found Java $JAVA_VERSION"
    echo "   Download from: https://adoptium.net/temurin/releases/"
    exit 1
fi

echo "✅ Java $JAVA_VERSION detected"

# Build the project
echo "📦 Compiling and packaging..."
./mvnw clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "🚀 To run the service:"
    echo "   ./mvnw spring-boot:run"
    echo ""
    echo "   Or run the JAR directly:"
    echo "   java -jar target/resource-quota-service-1.0.0.jar"
    echo ""
    echo "🌐 Service will be available at:"
    echo "   http://localhost:8080/quota-service/api/v1/quotas/health"
    echo ""
    echo "📊 Load test script:"
    echo "   ./load-test.sh"
else
    echo "❌ Build failed"
    exit 1
fi
