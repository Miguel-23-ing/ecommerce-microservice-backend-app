Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   ESTADO DE MINIKUBE Y SERVICIOS" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "1. Estado de Minikube:" -ForegroundColor Yellow
minikube status

Write-Host "`n2. Pods en ecommerce-microservices:" -ForegroundColor Yellow
kubectl get pods -n ecommerce-microservices | Select-String "NAME|Running|Pending"

Write-Host "`n3. Servicios desplegados:" -ForegroundColor Yellow
kubectl get services -n ecommerce-microservices -o custom-columns=NAME:.metadata.name,TYPE:.spec.type,PORT:.spec.ports[0].port

Write-Host "`n4. API Gateway Status:" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -ErrorAction Stop
    Write-Host "   Status: $($response.status)" -ForegroundColor Green
} catch {
    Write-Host "   Status: NO ACCESIBLE" -ForegroundColor Red
}

Write-Host "`n5. Resumen:" -ForegroundColor Yellow
$pods = kubectl get pods -n ecommerce-microservices --no-headers
$ready = ($pods | Select-String "1/1.*Running").Count
$total = ($pods | Measure-Object).Count
Write-Host "   Pods Ready: $ready/$total" -ForegroundColor $(if($ready -ge 6){"Green"}else{"Yellow"})
Write-Host "   API Gateway: http://localhost:8080" -ForegroundColor Green
Write-Host "   Eureka: kubectl port-forward service/service-discovery-service 8761:8761 -n ecommerce-microservices" -ForegroundColor Gray

Write-Host "`n========================================`n" -ForegroundColor Cyan
