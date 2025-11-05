# Script para iniciar todos los port-forwards necesarios de Minikube
# Mantiene los servicios accesibles en localhost

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INICIANDO PORT-FORWARDS DE MINIKUBE" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Servicios a exponer
$services = @(
    @{Name="API Gateway"; Service="api-gateway-service"; Port="8080"},
    @{Name="Zipkin"; Service="zipkin-service"; Port="9411"},
    @{Name="Eureka"; Service="service-discovery-service"; Port="8761"}
)

Write-Host "Servicios que serán expuestos:" -ForegroundColor Yellow
foreach ($svc in $services) {
    Write-Host "  • $($svc.Name): http://localhost:$($svc.Port)" -ForegroundColor White
}

Write-Host "`n⚠️  IMPORTANTE: NO CIERRES ESTA VENTANA" -ForegroundColor Yellow
Write-Host "Si cierras esta ventana, los servicios dejarán de ser accesibles`n" -ForegroundColor Yellow

Write-Host "Presiona Ctrl+C para detener todos los port-forwards`n" -ForegroundColor Cyan

# Iniciar port-forwards en jobs de PowerShell
$jobs = @()
foreach ($svc in $services) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Iniciando $($svc.Name)..." -ForegroundColor Green
    
    $scriptBlock = {
        param($service, $port)
        while ($true) {
            try {
                kubectl port-forward -n ecommerce-microservices svc/$service ${port}:${port} 2>&1
                Start-Sleep -Seconds 5
            }
            catch {
                Start-Sleep -Seconds 10
            }
        }
    }
    
    $job = Start-Job -ScriptBlock $scriptBlock -ArgumentList $svc.Service, $svc.Port
    $jobs += @{Name=$svc.Name; Job=$job; Port=$svc.Port}
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  ✅ PORT-FORWARDS ACTIVOS" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

foreach ($j in $jobs) {
    Write-Host "  • $($j.Name): http://localhost:$($j.Port)" -ForegroundColor White
}

Write-Host "`n========================================`n" -ForegroundColor Green

# Mantener el script corriendo y mostrar estado
try {
    while ($true) {
        Start-Sleep -Seconds 30
        
        # Verificar estado de los jobs
        $allRunning = $true
        foreach ($j in $jobs) {
            $jobState = (Get-Job -Id $j.Job.Id).State
            if ($jobState -ne "Running") {
                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ⚠️  $($j.Name) dejó de correr, reiniciando..." -ForegroundColor Yellow
                $allRunning = $false
                
                # Reiniciar el job
                Remove-Job -Id $j.Job.Id -Force
                $scriptBlock = {
                    param($service, $port)
                    while ($true) {
                        try {
                            kubectl port-forward -n ecommerce-microservices svc/$service ${port}:${port} 2>&1
                            Start-Sleep -Seconds 5
                        }
                        catch {
                            Start-Sleep -Seconds 10
                        }
                    }
                }
                $newJob = Start-Job -ScriptBlock $scriptBlock -ArgumentList $j.Service, $j.Port
                $j.Job = $newJob
            }
        }
        
        if ($allRunning) {
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ✅ Todos los port-forwards están activos" -ForegroundColor Green
        }
    }
}
finally {
    Write-Host "`n`nDeteniendo port-forwards..." -ForegroundColor Yellow
    foreach ($j in $jobs) {
        Stop-Job -Id $j.Job.Id
        Remove-Job -Id $j.Job.Id -Force
    }
    Write-Host "✅ Port-forwards detenidos" -ForegroundColor Green
}
