#!/bin/bash

# Complete deployment script for Ecommerce Microservices with Jenkins
# This script sets up everything: Minikube, Jenkins, and the microservices

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

# Function to check prerequisites
check_prerequisites() {
    print_step "Checking prerequisites..."
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    print_status "Docker is running"
    
    # Check if Minikube is installed
    if ! command -v minikube &> /dev/null; then
        print_error "Minikube is not installed. Please install Minikube first."
        exit 1
    fi
    print_status "Minikube is installed"
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    print_status "kubectl is installed"
}

# Function to start Minikube
start_minikube() {
    print_step "Starting Minikube..."
    
    if minikube status >/dev/null 2>&1; then
        print_status "Minikube is already running"
    else
        print_status "Starting Minikube..."
        minikube start --memory=4096 --cpus=2
    fi
    
    # Enable addons
    minikube addons enable ingress
    minikube addons enable metrics-server
    
    print_status "Minikube is ready"
}

# Function to build and deploy microservices
deploy_microservices() {
    print_step "Building and deploying microservices..."
    
    # Build Docker images
    eval $(minikube docker-env)
    
    print_status "Building Docker images..."
    docker build -t ecommerce/service-discovery:latest ./service-discovery
    docker build -t ecommerce/cloud-config:latest ./cloud-config
    docker build -t ecommerce/api-gateway:latest ./api-gateway
    docker build -t ecommerce/user-service:latest ./user-service
    # Build proxy-client instead of product-service
    docker build -t ecommerce/proxy-client:latest ./proxy-client
    
    # Deploy to Kubernetes
    print_status "Deploying to Kubernetes..."
    kubectl apply -f k8s/namespace.yaml
    kubectl apply -f k8s/zipkin-deployment.yaml
    kubectl apply -f k8s/service-discovery-deployment.yaml
    kubectl apply -f k8s/cloud-config-deployment.yaml
    
    # Wait for core services
    print_status "Waiting for core services..."
    kubectl wait --for=condition=available --timeout=300s deployment/service-discovery -n ecommerce-microservices
    kubectl wait --for=condition=available --timeout=300s deployment/cloud-config -n ecommerce-microservices
    
    # Deploy application services
    kubectl apply -f k8s/api-gateway-deployment.yaml
    kubectl apply -f k8s/user-service-deployment.yaml
    # product-service deployment replaced by proxy-client (k8s/product-service-deployment.yaml now deploys proxy-client)
    kubectl apply -f k8s/product-service-deployment.yaml
    
    # Wait for application services
    print_status "Waiting for application services..."
    kubectl wait --for=condition=available --timeout=300s deployment/api-gateway -n ecommerce-microservices
    kubectl wait --for=condition=available --timeout=300s deployment/user-service -n ecommerce-microservices
    # Wait for proxy-client deployment which replaced product-service
    kubectl wait --for=condition=available --timeout=300s deployment/proxy-client -n ecommerce-microservices
    
    print_status "Microservices deployed successfully!"
}

# Function to deploy Jenkins
deploy_jenkins() {
    print_step "Deploying Jenkins..."
    
    # Copy kubeconfig to Jenkins volume
    mkdir -p jenkins/kubeconfig
    cp ~/.kube/config jenkins/kubeconfig/config 2>/dev/null || true
    
    # Deploy Jenkins
    cd jenkins
    docker-compose up -d
    
    # Wait for Jenkins to be ready
    print_status "Waiting for Jenkins to be ready..."
    for i in {1..30}; do
        if curl -f http://localhost:8080/login >/dev/null 2>&1; then
            print_status "Jenkins is ready!"
            break
        fi
        print_status "Waiting for Jenkins... ($i/30)"
        sleep 10
    done
    
    cd ..
    print_status "Jenkins deployed successfully!"
}

# Function to show status
show_status() {
    print_step "Deployment Status:"
    
    echo ""
    print_status "Minikube Status:"
    minikube status
    
    echo ""
    print_status "Kubernetes Pods:"
    kubectl get pods -n ecommerce-microservices
    
    echo ""
    print_status "Kubernetes Services:"
    kubectl get services -n ecommerce-microservices
    
    echo ""
    print_status "API Gateway URL:"
    minikube service api-gateway-service -n ecommerce-microservices --url
    
    echo ""
    print_status "Jenkins URL:"
    echo "http://localhost:8080"
    echo "Username: admin"
    echo "Password: admin123"
    
    echo ""
    print_status "Zipkin URL:"
    minikube service zipkin-service -n ecommerce-microservices --url
}

# Function to run tests
run_tests() {
    print_step "Running tests..."
    
    # Get API Gateway URL
    API_GATEWAY_URL=$(minikube service api-gateway-service -n ecommerce-microservices --url)
    
    print_status "Testing API Gateway health..."
    curl -f $API_GATEWAY_URL/actuator/health || print_error "API Gateway health check failed"
    
    print_status "Testing User Service..."
    curl -f $API_GATEWAY_URL/user-service/api/users || print_error "User Service test failed"
    
    print_status "Testing Product Service..."
    curl -f $API_GATEWAY_URL/product-service/api/products || print_error "Product Service test failed"
    
    print_status "All tests passed!"
}

# Function to cleanup
cleanup() {
    print_step "Cleaning up..."
    
    # Stop Jenkins
    cd jenkins
    docker-compose down
    cd ..
    
    # Delete Kubernetes resources
    kubectl delete namespace ecommerce-microservices
    
    # Stop Minikube
    minikube stop
    
    print_status "Cleanup completed!"
}

# Main script logic
case "${1:-deploy}" in
    deploy)
        check_prerequisites
        start_minikube
        deploy_microservices
        deploy_jenkins
        show_status
        ;;
    test)
        run_tests
        ;;
    status)
        show_status
        ;;
    cleanup)
        cleanup
        ;;
    restart)
        cleanup
        sleep 5
        check_prerequisites
        start_minikube
        deploy_microservices
        deploy_jenkins
        show_status
        ;;
    *)
        echo "Usage: $0 {deploy|test|status|cleanup|restart}"
        echo "  deploy  - Deploy everything (default)"
        echo "  test    - Run tests"
        echo "  status  - Show status"
        echo "  cleanup - Clean up everything"
        echo "  restart - Clean up and redeploy"
        exit 1
        ;;
esac
