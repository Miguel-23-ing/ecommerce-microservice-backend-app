pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'ecommerce'
        KUBECONFIG = '/var/jenkins_home/.kube/config'
        MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                }
            }
        }
        
        stage('Build & Test') {
            parallel {
                stage('Build User Service') {
                    steps {
                        dir('user-service') {
                            sh 'mvn clean compile test package -DskipTests'
                        }
                    }
                }
                stage('Build Product Service') {
                    steps {
                        dir('product-service') {
                            sh 'mvn clean compile test package -DskipTests'
                        }
                    }
                }
                stage('Build API Gateway') {
                    steps {
                        dir('api-gateway') {
                            sh 'mvn clean compile test package -DskipTests'
                        }
                    }
                }
                stage('Build Service Discovery') {
                    steps {
                        dir('service-discovery') {
                            sh 'mvn clean compile test package -DskipTests'
                        }
                    }
                }
                stage('Build Cloud Config') {
                    steps {
                        dir('cloud-config') {
                            sh 'mvn clean compile test package -DskipTests'
                        }
                    }
                }
            }
        }
        
        stage('Unit Tests') {
            parallel {
                stage('User Service Tests') {
                    steps {
                        dir('user-service') {
                            sh 'mvn test'
                        }
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Product Service Tests') {
                    steps {
                        dir('product-service') {
                            sh 'mvn test'
                        }
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('API Gateway Tests') {
                    steps {
                        dir('api-gateway') {
                            sh 'mvn test'
                        }
                    }
                    post {
                        always {
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    def services = ['user-service', 'product-service', 'api-gateway', 'service-discovery', 'cloud-config']
                    
                    services.each { service ->
                        sh "docker build -t ${DOCKER_REGISTRY}/${service}:${env.GIT_COMMIT_SHORT} ./${service}"
                        sh "docker tag ${DOCKER_REGISTRY}/${service}:${env.GIT_COMMIT_SHORT} ${DOCKER_REGISTRY}/${service}:latest"
                    }
                }
            }
        }
        
        stage('Deploy to Minikube') {
            steps {
                script {
                    sh 'kubectl apply -f k8s/namespace.yaml'
                    sh 'kubectl apply -f k8s/zipkin-deployment.yaml'
                    sh 'kubectl apply -f k8s/service-discovery-deployment.yaml'
                    sh 'kubectl apply -f k8s/cloud-config-deployment.yaml'
                    
                    // Wait for core services
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/service-discovery -n ecommerce-microservices'
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/cloud-config -n ecommerce-microservices'
                    
                    sh 'kubectl apply -f k8s/api-gateway-deployment.yaml'
                    sh 'kubectl apply -f k8s/user-service-deployment.yaml'
                    sh 'kubectl apply -f k8s/product-service-deployment.yaml'
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                script {
                    // Wait for services to be ready
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/api-gateway -n ecommerce-microservices'
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/user-service -n ecommerce-microservices'
                    sh 'kubectl wait --for=condition=available --timeout=300s deployment/product-service -n ecommerce-microservices'
                    
                    // Get API Gateway URL
                    def apiGatewayUrl = sh(
                        script: 'minikube service api-gateway-service -n ecommerce-microservices --url',
                        returnStdout: true
                    ).trim()
                    
                    env.API_GATEWAY_URL = apiGatewayUrl
                    
                    // Run integration tests
                    sh '''
                        # Test API Gateway health
                        curl -f ${API_GATEWAY_URL}/actuator/health || exit 1
                        
                        # Test User Service through API Gateway
                        curl -f ${API_GATEWAY_URL}/user-service/api/users || exit 1
                        
                        # Test Product Service through API Gateway
                        curl -f ${API_GATEWAY_URL}/product-service/api/products || exit 1
                        
                        echo "Integration tests passed!"
                    '''
                }
            }
        }
        
        stage('Performance Tests') {
            steps {
                script {
                    sh '''
                        # Simple performance test using curl
                        echo "Running performance tests..."
                        
                        # Test API Gateway response time
                        for i in {1..10}; do
                            curl -w "Time: %{time_total}s\n" -o /dev/null -s ${API_GATEWAY_URL}/actuator/health
                        done
                        
                        echo "Performance tests completed!"
                    '''
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Clean up Docker images
                sh 'docker system prune -f'
                
                // Show service status
                sh 'kubectl get pods -n ecommerce-microservices'
                sh 'kubectl get services -n ecommerce-microservices'
            }
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
