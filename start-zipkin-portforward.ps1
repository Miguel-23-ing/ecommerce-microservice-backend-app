# Script para mantener Zipkin accesible permanentemente
# Mantiene el port-forward activo en segundo plano

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INICIANDO ZIPKIN PORT-FORWARD" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Verificando que Zipkin esté corriendo..." -ForegroundColor Yellow
$zipkinPod = kubectl get pods -n ecommerce-microservices -l app=zipkin -o jsonpath='{.items[0].metadata.name}'

if ([string]::IsNullOrEmpty($zipkinPod)) {
    Write-Host "❌ Zipkin no está corriendo en Minikube" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Pod encontrado: $zipkinPod" -ForegroundColor Green

Write-Host "`n🔗 Iniciando port-forward permanente..." -ForegroundColor Cyan
Write-Host "Zipkin estará disponible en: http://localhost:9411" -ForegroundColor White
Write-Host "`n⚠️  IMPORTANTE: NO CIERRES ESTA VENTANA" -ForegroundColor Yellow
Write-Host "Si cierras esta ventana, Zipkin dejará de ser accesible`n" -ForegroundColor Yellow

Write-Host "========================================" -ForegroundColor Green
Write-Host "  PORT-FORWARD ACTIVO" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green

# Mantener el port-forward corriendo (se reconecta automáticamente si falla)
while ($true) {
    try {
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Iniciando port-forward..." -ForegroundColor Cyan
        kubectl port-forward -n ecommerce-microservices svc/zipkin-service 9411:9411
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Port-forward terminó, reiniciando en 5s..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
    catch {
        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Reintentando en 10 segundos..." -ForegroundColor Yellow
        Start-Sleep -Seconds 10
    }
}
