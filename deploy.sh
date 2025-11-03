#!/bin/bash

# Deploy script for Ecommerce Microservices
# This script follows the guide to deploy services in the correct order

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Function to check if container is healthy
check_container_health() {
    local container_name=$1
    local max_attempts=30
    local attempt=1
    
    print_status "Waiting for $container_name to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker ps --filter "name=$container_name" --filter "health=healthy" | grep -q $container_name; then
            print_status "$container_name is healthy!"
            return 0
        fi
        
        print_status "Attempt $attempt/$max_attempts - $container_name not ready yet..."
        sleep 10
        ((attempt++))
    done
    
    print_error "$container_name failed to become healthy after $max_attempts attempts"
    return 1
}

# Function to show container logs
show_logs() {
    local container_name=$1
    print_status "Showing logs for $container_name:"
    docker logs --tail=20 $container_name
}

# Main deployment function
deploy() {
    print_step "Starting Ecommerce Microservices Deployment"
    
    # Step 1: Deploy core services
    print_step "Step 1: Deploying core services (zipkin, service-discovery, cloud-config)"
    docker-compose -f core.yml up -d
    
    # Wait for core services to be healthy
    check_container_health "zipkin-container"
    check_container_health "service-discovery-container"
    check_container_health "cloud-config-container"
    
    # Show logs for debugging
    show_logs "service-discovery-container"
    show_logs "cloud-config-container"
    
    # Step 2: Deploy application services
    print_step "Step 2: Deploying application services (api-gateway, user-service, product-service)"
    docker-compose -f compose.yml up -d
    
    # Wait for application services to be healthy
    check_container_health "api-gateway-container"
    check_container_health "user-service-container"
    check_container_health "product-service-container"
    
    # Show final status
    print_step "Deployment completed successfully!"
    print_status "Services are running on:"
    print_status "  - API Gateway: http://localhost:8080"
    print_status "  - User Service: http://localhost:8700"
    print_status "  - Product Service: http://localhost:8500"
    print_status "  - Service Discovery: http://localhost:8761"
    print_status "  - Cloud Config: http://localhost:9296"
    print_status "  - Zipkin: http://localhost:9411"
    
    print_status "Test endpoints:"
    print_status "  - Products: http://localhost:8080/product-service/api/products"
    print_status "  - Users: http://localhost:8080/user-service/api/users"
    print_status "  - Health: http://localhost:8080/actuator/health"
}

# Function to stop all services
stop() {
    print_step "Stopping all services..."
    docker-compose -f compose.yml down
    docker-compose -f core.yml down
    print_status "All services stopped!"
}

# Function to show service status
status() {
    print_step "Service Status:"
    docker ps --filter "name=container" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# Function to show logs
logs() {
    local service=$1
    if [ -z "$service" ]; then
        print_error "Please specify a service name"
        print_status "Available services: zipkin-container, service-discovery-container, cloud-config-container, api-gateway-container, user-service-container, product-service-container"
        exit 1
    fi
    
    docker logs -f $service
}

# Main script logic
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    stop)
        stop
        ;;
    status)
        status
        ;;
    logs)
        logs $2
        ;;
    restart)
        stop
        sleep 5
        deploy
        ;;
    *)
        echo "Usage: $0 {deploy|stop|status|logs|restart}"
        echo "  deploy  - Deploy all services (default)"
        echo "  stop    - Stop all services"
        echo "  status  - Show service status"
        echo "  logs    - Show logs for a specific service"
        echo "  restart - Restart all services"
        exit 1
        ;;
esac
