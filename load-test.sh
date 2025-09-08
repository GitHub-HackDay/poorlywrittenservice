#!/bin/bash

# Load Test Script for Resource Quota Service
# This script demonstrates high-concurrency testing

SERVICE_URL="http://localhost:8080/quota-service/api/v1/quotas"

echo "🚀 Starting Resource Quota Service Load Test"

# Create test quotas for multiple resources
echo "📝 Creating quotas for 100 test resources..."
for i in {1..100}; do
    curl -s -X POST "$SERVICE_URL" \
        -H "Content-Type: application/json" \
        -d "{\"resourceId\":\"load-test-$i\",\"maxRequests\":1000,\"timeWindowSeconds\":3600}" > /dev/null &
done
wait

echo "✅ Quotas created"

# Function to test quota consumption
test_quota_consumption() {
    local resource_id=$1
    local requests=$2
    local allowed=0
    local denied=0
    
    for ((j=1; j<=requests; j++)); do
        response=$(curl -s -X POST "$SERVICE_URL/$resource_id/check-and-consume")
        if echo "$response" | grep -q '"allowed":true'; then
            ((allowed++))
        else
            ((denied++))
        fi
    done
    
    echo "Resource $resource_id: $allowed allowed, $denied denied"
}

# Concurrent load test
echo "⚡ Running concurrent load test (10 resources, 50 requests each)..."
for i in {1..10}; do
    test_quota_consumption "load-test-$i" 50 &
done
wait

# Test quota checking without consumption
echo "🔍 Testing quota checks without consumption..."
for i in {1..5}; do
    response=$(curl -s "$SERVICE_URL/load-test-$i/check?requestCount=10")
    echo "Check load-test-$i: $(echo $response | jq -r '.message')"
done

# Test quota reset
echo "🔄 Testing quota reset..."
curl -s -X POST "$SERVICE_URL/load-test-1/reset" | jq -r '.message'

# Get service health
echo "💚 Service health check..."
curl -s "$SERVICE_URL/health" | jq '.'

echo "🎉 Load test completed!"
