Write-Host ""
Write-Host "Generando trazas a traves de PROXY-CLIENT..." -ForegroundColor Cyan
Write-Host ""

$authUrl = "http://localhost:8900/app/api/authenticate"
$proxyUrl = "http://localhost:8900/app/api"

# 1. Autenticar
Write-Host "Autenticando con usuario Miguel..." -ForegroundColor Yellow
$credentials = @{
    username = "miguel"
    password = "password123"
} | ConvertTo-Json

try {
    $authResponse = Invoke-RestMethod -Uri $authUrl -Method Post -Body $credentials -ContentType "application/json" -TimeoutSec 20
    $token = $authResponse.jwtToken
    $headers = @{ Authorization = "Bearer $token" }
    Write-Host "   Token JWT obtenido!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "   Error de autenticacion: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "   Verifica que proxy-client este funcionando." -ForegroundColor Yellow
    Write-Host ""
    exit
}

# 2. Generar trazas
Write-Host "Generando 20 trazas distribuidas..." -ForegroundColor Yellow
Write-Host ""

$success = 0
for ($i = 1; $i -le 20; $i++) {
    Write-Host "   Traza $i/20: " -NoNewline
    
    switch ($i % 6) {
        0 { 
            try { 
                Invoke-RestMethod -Uri "$proxyUrl/users" -Headers $headers -TimeoutSec 10 | Out-Null
                Write-Host "users ✅" -ForegroundColor Green
                $success++
            } catch { 
                Write-Host "users ⚠️" -ForegroundColor Yellow 
            }
        }
        1 { 
            try { 
                Invoke-RestMethod -Uri "$proxyUrl/products" -Headers $headers -TimeoutSec 10 | Out-Null
                Write-Host "products ✅" -ForegroundColor Green
                $success++
            } catch { 
                Write-Host "products ⚠️" -ForegroundColor Yellow 
            }
        }
        2 { 
            try { 
                Invoke-RestMethod -Uri "$proxyUrl/orders" -Headers $headers -TimeoutSec 10 | Out-Null
                Write-Host "orders ✅" -ForegroundColor Green
                $success++
            } catch { 
                Write-Host "orders ⚠️" -ForegroundColor Yellow 
            }
        }
        3 { 
            try { 
                Invoke-RestMethod -Uri "$proxyUrl/payments" -Headers $headers -TimeoutSec 10 | Out-Null
                Write-Host "payments ✅" -ForegroundColor Green
                $success++
            } catch { 
                Write-Host "payments ⚠️" -ForegroundColor Yellow 
            }
        }
        4 { 
            try { 
                Invoke-RestMethod -Uri "$proxyUrl/shipping" -Headers $headers -TimeoutSec 10 | Out-Null
                Write-Host "shipping ✅" -ForegroundColor Green
                $success++
            } catch { 
                Write-Host "shipping ⚠️" -ForegroundColor Yellow 
            }
        }
        5 { 
            try { 
                Invoke-RestMethod -Uri "$proxyUrl/favourites" -Headers $headers -TimeoutSec 10 | Out-Null
                Write-Host "favourites ✅" -ForegroundColor Green
                $success++
            } catch { 
                Write-Host "favourites ⚠️" -ForegroundColor Yellow 
            }
        }
    }
    
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Trazas generadas exitosamente: $success/20" -ForegroundColor Green
Write-Host ""
Write-Host "   Arquitectura completa:" -ForegroundColor Cyan
Write-Host "   API-Gateway -> Proxy-Client -> Microservicios" -ForegroundColor White
Write-Host ""
Write-Host "Abre Zipkin: http://localhost:9411/zipkin/" -ForegroundColor Yellow
Write-Host ""
Write-Host "   Pasos en Zipkin:" -ForegroundColor Gray
Write-Host "   1. Haz clic en 'Run Query' o 'Find Traces'" -ForegroundColor Gray
Write-Host "   2. Veras las $success trazas generadas" -ForegroundColor Gray
Write-Host "   3. Haz clic en cualquier traza para ver:" -ForegroundColor Gray
Write-Host "      - API Gateway recibiendo la peticion" -ForegroundColor Gray
Write-Host "      - Proxy-Client procesando" -ForegroundColor Gray
Write-Host "      - Microservicio respondiendo" -ForegroundColor Gray
Write-Host "   4. En Dependencies veras el diagrama completo" -ForegroundColor Gray
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
