# Script para compilar el proyecto completo y luego crear las imágenes Docker
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "COMPILANDO PROYECTO COMPLETO CON MAVEN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Primero compilar todo el proyecto desde la raíz
Write-Host "Compilando proyecto multimodulo con Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: La compilación Maven falló" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CONSTRUCCION DE IMAGENES DOCKER" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$services = @("cloud-config", "service-discovery", "user-service", "product-service", "shipping-service", "order-service", "payment-service", "api-gateway")

foreach ($service in $services) {
    Write-Host "Construyendo imagen $service..." -ForegroundColor Yellow
    
    Push-Location ".\$service"
    docker build -t "ecommerce-${service}:dev" --build-arg ENVIRONMENT=dev .
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "OK: $service" -ForegroundColor Green
    } else {
        Write-Host "ERROR: $service" -ForegroundColor Red
    }
    
    Pop-Location
    Write-Host ""
}

Write-Host "========================================"
Write-Host "Imagenes creadas:"
docker images | Select-String "ecommerce.*:dev"
Write-Host ""
Write-Host "Para iniciar: docker-compose -f docker-compose-dev.yml up -d"
Write-Host "========================================"
