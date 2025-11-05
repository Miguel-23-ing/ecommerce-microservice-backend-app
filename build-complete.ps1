# Script automatizado para compilar y crear imágenes Docker
# ========================================

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "PASO 1: COMPILANDO CON MAVEN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Compilar todo el proyecto
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilación fallida" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PASO 2: CONSTRUYENDO IMAGENES DOCKER" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Usar docker-compose para construir
docker-compose -f docker-compose-dev.yml build

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "ÉXITO: Todas las imágenes creadas" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Imágenes disponibles:"
    docker images | Select-String "ecommerce.*dev"
    Write-Host ""
    Write-Host "Para iniciar los servicios:"
    Write-Host "  docker-compose -f docker-compose-dev.yml up -d" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Para ver logs:"
    Write-Host "  docker-compose -f docker-compose-dev.yml logs -f" -ForegroundColor Yellow
} else {
    Write-Host "ERROR: Construcción de imágenes fallida" -ForegroundColor Red
    exit 1
}
