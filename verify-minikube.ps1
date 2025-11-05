# Script para verificar el estado y acceder a los servicios en Minikube
# Autor: Copilot
# Fecha: 2025-11-05

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  Verificación de Servicios en Minikube" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar pods
Write-Host "📦 PODS:" -ForegroundColor Yellow
kubectl get pods -n ecommerce-microservices -o wide
Write-Host ""

# 2. Verificar servicios
Write-Host "🌐 SERVICIOS:" -ForegroundColor Yellow
kubectl get services -n ecommerce-microservices
Write-Host ""

# 3. Verificar deployments
Write-Host "🚀 DEPLOYMENTS:" -ForegroundColor Yellow
kubectl get deployments -n ecommerce-microservices
Write-Host ""

# 4. Mostrar URLs de acceso
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  URLs de Acceso" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Para acceder a los servicios, ejecuta:" -ForegroundColor Green
Write-Host ""
Write-Host "API Gateway (puerto 8080):" -ForegroundColor Yellow
Write-Host "  kubectl port-forward service/api-gateway-service 8080:8080 -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
Write-Host "Service Discovery - Eureka (puerto 8761):" -ForegroundColor Yellow
Write-Host "  kubectl port-forward service/service-discovery-service 8761:8761 -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
Write-Host "Zipkin (puerto 9411):" -ForegroundColor Yellow
Write-Host "  kubectl port-forward service/zipkin-service 9411:9411 -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
Write-Host "Cloud Config (puerto 9296):" -ForegroundColor Yellow
Write-Host "  kubectl port-forward service/cloud-config-service 9296:9296 -n ecommerce-microservices" -ForegroundColor White
Write-Host ""

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  Comandos Útiles" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Ver logs de un pod:" -ForegroundColor Green
Write-Host "  kubectl logs <pod-name> -n ecommerce-microservices -f" -ForegroundColor White
Write-Host ""
Write-Host "Ver descripción de un pod:" -ForegroundColor Green
Write-Host "  kubectl describe pod <pod-name> -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
Write-Host "Reiniciar un deployment:" -ForegroundColor Green
Write-Host "  kubectl rollout restart deployment/<deployment-name> -n ecommerce-microservices" -ForegroundColor White
Write-Host ""
Write-Host "Eliminar todos los recursos:" -ForegroundColor Green
Write-Host "  kubectl delete namespace ecommerce-microservices" -ForegroundColor White
Write-Host ""
