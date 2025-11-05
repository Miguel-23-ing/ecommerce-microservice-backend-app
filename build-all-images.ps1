# Script para construir todas las imágenes Docker
Write-Host "========================================"
Write-Host "CONSTRUCCION DE IMAGENES DOCKER"
Write-Host "========================================"
Write-Host ""

$services = @("cloud-config", "service-discovery", "user-service", "product-service", "shipping-service", "order-service", "payment-service", "api-gateway")

foreach ($service in $services) {
    Write-Host "Construyendo $service..." -ForegroundColor Yellow
    
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
docker images | Select-String "ecommerce"
Write-Host ""
Write-Host "Para iniciar: docker-compose -f docker-compose-dev.yml up -d"
Write-Host "========================================"
