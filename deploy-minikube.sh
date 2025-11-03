#!/bin/bash

# Deploy script for Ecommerce Microservices on Minikube
# This script builds Docker images and deploys to Minikube

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

# Function to check if Minikube is running
check_minikube() {
    if ! minikube status >/dev/null 2>&1; then
        print_error "Minikube is not running. Please start Minikube first:"
        print_status "minikube start"
        exit 1
    fi
    print_status "Minikube is running"
}

# Function to build Docker images
build_images() {
    print_step "Building Docker images for Minikube..."
    
    # Set Minikube Docker environment
    eval $(minikube docker-env)
    
    # Build images
    print_status "Building service-discovery..."
    docker build -t ecommerce/service-discovery:latest ./service-discovery
    
    print_status "Building cloud-config..."
    docker build -t ecommerce/cloud-config:latest ./cloud-config
    
    print_status "Building api-gateway..."
    docker build -t ecommerce/api-gateway:latest ./api-gateway
    
    print_status "Building user-service..."
    docker build -t ecommerce/user-service:latest ./user-service
    
    print_status "Building proxy-client..."
    docker build -t ecommerce/proxy-client:latest ./proxy-client
    
    print_status "All images built successfully!"
}

# Function to deploy to Kubernetes
deploy_k8s() {
    print_step "Deploying to Kubernetes..."
    
    # Create namespace
    print_status "Creating namespace..."
    kubectl apply -f k8s/namespace.yaml
    
    # Deploy core services first
    print_status "Deploying core services..."
    kubectl apply -f k8s/zipkin-deployment.yaml
    kubectl apply -f k8s/service-discovery-deployment.yaml
    kubectl apply -f k8s/cloud-config-deployment.yaml
    
    # Wait for core services to be ready
    print_status "Waiting for core services to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/service-discovery -n ecommerce-microservices
    kubectl wait --for=condition=available --timeout=300s deployment/cloud-config -n ecommerce-microservices
    
    # Deploy application services
    print_status "Deploying application services..."
    kubectl apply -f k8s/api-gateway-deployment.yaml
    kubectl apply -f k8s/user-service-deployment.yaml
    # product-service deployment replaced by proxy-client (k8s/product-service-deployment.yaml now deploys proxy-client)
    kubectl apply -f k8s/product-service-deployment.yaml
    
    # Wait for application services to be ready
    print_status "Waiting for application services to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/api-gateway -n ecommerce-microservices
    kubectl wait --for=condition=available --timeout=300s deployment/user-service -n ecommerce-microservices
    # Wait for proxy-client (deployed via product-service-deployment.yaml but named proxy-client)
    kubectl wait --for=condition=available --timeout=300s deployment/proxy-client -n ecommerce-microservices
    
    print_status "All services deployed successfully!"
}

# Function to show service status
show_status() {
    print_step "Service Status:"
    kubectl get pods -n ecommerce-microservices
    echo ""
    print_step "Services:"
    kubectl get services -n ecommerce-microservices
    echo ""
    print_step "API Gateway URL:"
    minikube service api-gateway-service -n ecommerce-microservices --url
}

# Function to show logs
show_logs() {
    local service=$1
    if [ -z "$service" ]; then
        print_error "Please specify a service name"
        print_status "Available services: zipkin, service-discovery, cloud-config, api-gateway, user-service, proxy-client"
        exit 1
    fi
    
    kubectl logs -f deployment/$service -n ecommerce-microservices
}

# Function to clean up
cleanup() {
    print_step "Cleaning up deployments..."
    kubectl delete namespace ecommerce-microservices
    print_status "Cleanup completed!"
}

# Main script logic
case "${1:-deploy}" in
    deploy)
        check_minikube
        build_images
        deploy_k8s
        show_status
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs $2
        ;;
    cleanup)
        cleanup
        ;;
    restart)
        cleanup
        sleep 5
        check_minikube
        build_images
        deploy_k8s
        show_status
        ;;
    *)
        echo "Usage: $0 {deploy|status|logs|cleanup|restart}"
        echo "  deploy  - Build images and deploy all services (default)"
        echo "  status  - Show service status"
        echo "  logs    - Show logs for a specific service"
        echo "  cleanup - Remove all deployments"
        echo "  restart - Cleanup and redeploy"
        exit 1
        ;;
esac
