# Script para ejecutar tests E2E
# Asegúrate de que todos los servicios estén corriendo antes de ejecutar

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  EJECUTANDO TESTS E2E" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Verificar que Maven esté disponible
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Maven no encontrado. Por favor instala Maven." -ForegroundColor Red
    exit 1
}

Write-Host "📋 Verificando servicios..." -ForegroundColor Yellow

# Verificar que el API Gateway esté disponible
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ API Gateway está disponible" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  API Gateway no está disponible en http://localhost:8080" -ForegroundColor Yellow
    Write-Host "   Los tests E2E requieren que todos los servicios estén corriendo." -ForegroundColor Yellow
    Write-Host "   Iniciando servicios..." -ForegroundColor Yellow
    
    $continue = Read-Host "`n¿Deseas continuar de todos modos? (s/n)"
    if ($continue -ne 's' -and $continue -ne 'S') {
        Write-Host "Ejecución cancelada." -ForegroundColor Red
        exit 0
    }
}

Write-Host "`n🧪 Ejecutando tests E2E..." -ForegroundColor Cyan

# Ejecutar tests E2E
mvn test -pl e2e-tests -Dapi.gateway.url=http://localhost:8080

$exitCode = $LASTEXITCODE

Write-Host "`n========================================" -ForegroundColor Cyan
if ($exitCode -eq 0) {
    Write-Host "  ✅ TESTS E2E COMPLETADOS EXITOSAMENTE" -ForegroundColor Green
} else {
    Write-Host "  ❌ TESTS E2E FALLARON" -ForegroundColor Red
}
Write-Host "========================================`n" -ForegroundColor Cyan

exit $exitCode
