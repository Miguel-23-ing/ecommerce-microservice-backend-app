# Script para desplegar microservicios en Minikube
# Autor: Copilot
# Fecha: 2025-11-05

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  Despliegue de Microservicios en Minikube" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar que Minikube esté corriendo
Write-Host "[1/7] Verificando estado de Minikube..." -ForegroundColor Yellow
$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Minikube no está corriendo. Iniciándolo..." -ForegroundColor Red
    minikube start --driver=docker --cpus=4 --memory=8192
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Error al iniciar Minikube" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✅ Minikube está corriendo" -ForegroundColor Green
}
Write-Host ""

# 2. Configurar Docker para usar el daemon de Minikube
Write-Host "[2/7] Configurando Docker para usar daemon de Minikube..." -ForegroundColor Yellow
& minikube -p minikube docker-env --shell powershell | Invoke-Expression
Write-Host "✅ Docker configurado para Minikube" -ForegroundColor Green
Write-Host ""

# 3. Construir imágenes Docker dentro de Minikube
Write-Host "[3/7] Construyendo imágenes Docker en Minikube..." -ForegroundColor Yellow
Write-Host "Esto puede tomar varios minutos..." -ForegroundColor Gray

# Compilar JARs primero
Write-Host "  - Compilando JARs con Maven..." -ForegroundColor Gray
.\mvnw clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error al compilar con Maven" -ForegroundColor Red
    exit 1
}

# Construir imágenes
Write-Host "  - Construyendo imágenes Docker..." -ForegroundColor Gray
docker-compose build 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error al construir imágenes Docker" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Imágenes Docker construidas" -ForegroundColor Green
Write-Host ""

# 4. Crear namespace
Write-Host "[4/7] Creando namespace..." -ForegroundColor Yellow
kubectl apply -f k8s/namespace.yaml
Write-Host "✅ Namespace creado/actualizado" -ForegroundColor Green
Write-Host ""

# 5. Desplegar servicios de infraestructura primero
Write-Host "[5/7] Desplegando servicios de infraestructura..." -ForegroundColor Yellow
kubectl apply -f k8s/zipkin-deployment.yaml
kubectl apply -f k8s/cloud-config-deployment.yaml
kubectl apply -f k8s/service-discovery-deployment.yaml

Write-Host "  - Esperando a que servicios de infraestructura estén listos..." -ForegroundColor Gray
Start-Sleep -Seconds 30
Write-Host "✅ Servicios de infraestructura desplegados" -ForegroundColor Green
Write-Host ""

# 6. Desplegar microservicios de negocio
Write-Host "[6/7] Desplegando microservicios de negocio..." -ForegroundColor Yellow
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/product-service-deployment.yaml
kubectl apply -f k8s/favourite-service-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/shipping-service-deployment.yaml
kubectl apply -f k8s/api-gateway-deployment.yaml

Write-Host "  - Esperando a que microservicios estén listos..." -ForegroundColor Gray
Start-Sleep -Seconds 20
Write-Host "✅ Microservicios de negocio desplegados" -ForegroundColor Green
Write-Host ""

# 7. Verificar estado de los pods
Write-Host "[7/7] Verificando estado de los pods..." -ForegroundColor Yellow
kubectl get pods -n ecommerce-microservices
Write-Host ""

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  Despliegue Completado" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para ver los servicios ejecuta:" -ForegroundColor Green
Write-Host "  kubectl get services -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
Write-Host "Para ver los logs de un pod:" -ForegroundColor Green
Write-Host "  kubectl logs <pod-name> -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
Write-Host "Para acceder al API Gateway:" -ForegroundColor Green
Write-Host "  kubectl port-forward service/api-gateway-service 8080:8080 -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
