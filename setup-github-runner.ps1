# ============================================================================
# 🚀 Script de Configuración Rápida de GitHub Actions Runner
# ============================================================================
# Este script automatiza la descarga e instalación del GitHub Actions Runner
# para Windows
# ============================================================================

param(
    [Parameter(Mandatory=$true, HelpMessage="Token de autenticación de GitHub (obtenlo de Settings → Actions → Runners → New runner)")]
    [string]$Token,
    
    [Parameter(Mandatory=$false)]
    [string]$RunnerName = "windows-local-runner",
    
    [Parameter(Mandatory=$false)]
    [string]$RunnerDirectory = "C:\actions-runner"
)

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   🚀 GitHub Actions Runner - Setup Script            ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Verificar permisos de administrador
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
$isAdmin = $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "⚠️  ADVERTENCIA: Este script requiere permisos de administrador" -ForegroundColor Yellow
    Write-Host "   Por favor, ejecuta PowerShell como Administrador" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   Puedes continuar sin permisos de admin, pero no podrás instalar el servicio" -ForegroundColor Yellow
    $continue = Read-Host "¿Deseas continuar de todos modos? (s/n)"
    if ($continue -ne "s") {
        exit
    }
}

Write-Host "📋 Configuración:" -ForegroundColor Cyan
Write-Host "   Directorio: $RunnerDirectory" -ForegroundColor White
Write-Host "   Nombre del Runner: $RunnerName" -ForegroundColor White
Write-Host ""

# Paso 1: Crear directorio
Write-Host "📁 Paso 1: Creando directorio..." -ForegroundColor Cyan
if (-not (Test-Path $RunnerDirectory)) {
    New-Item -Path $RunnerDirectory -ItemType Directory -Force | Out-Null
    Write-Host "   ✅ Directorio creado: $RunnerDirectory" -ForegroundColor Green
} else {
    Write-Host "   ℹ️  El directorio ya existe" -ForegroundColor Yellow
}

Set-Location $RunnerDirectory

# Paso 2: Descargar runner
Write-Host ""
Write-Host "📥 Paso 2: Descargando GitHub Actions Runner..." -ForegroundColor Cyan

$runnerVersion = "2.329.0"
$runnerZip = "actions-runner-win-x64-$runnerVersion.zip"
$downloadUrl = "https://github.com/actions/runner/releases/download/v$runnerVersion/$runnerZip"

if (-not (Test-Path $runnerZip)) {
    try {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $runnerZip
        Write-Host "   ✅ Runner descargado" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ Error al descargar el runner: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "   ℹ️  El archivo ya existe, omitiendo descarga" -ForegroundColor Yellow
}

# Paso 3: Extraer
Write-Host ""
Write-Host "📦 Paso 3: Extrayendo archivos..." -ForegroundColor Cyan

if (-not (Test-Path ".\config.cmd")) {
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD\$runnerZip", "$PWD")
        Write-Host "   ✅ Archivos extraídos" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ Error al extraer archivos: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "   ℹ️  Los archivos ya están extraídos" -ForegroundColor Yellow
}

# Paso 4: Configurar runner
Write-Host ""
Write-Host "⚙️  Paso 4: Configurando runner..." -ForegroundColor Cyan

$repoUrl = "https://github.com/Miguel-23-ing/ecommerce-microservice-backend-app"
$labels = "self-hosted,Windows,X64,java,docker,kubernetes"

Write-Host "   Repository: $repoUrl" -ForegroundColor White
Write-Host "   Labels: $labels" -ForegroundColor White
Write-Host ""

try {
    # Ejecutar configuración
    $configArgs = @(
        "--url", $repoUrl,
        "--token", $Token,
        "--name", $RunnerName,
        "--labels", $labels,
        "--work", "_work",
        "--unattended"
    )
    
    & .\config.cmd $configArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ Runner configurado exitosamente" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Error en la configuración del runner" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ❌ Error al configurar el runner: $_" -ForegroundColor Red
    exit 1
}

# Paso 5: Instalar como servicio (solo si es admin)
Write-Host ""
if ($isAdmin) {
    Write-Host "🔧 Paso 5: Instalando como servicio de Windows..." -ForegroundColor Cyan
    
    try {
        & .\svc.cmd install
        & .\svc.cmd start
        
        Write-Host "   ✅ Servicio instalado e iniciado" -ForegroundColor Green
        Write-Host ""
        Write-Host "   El runner se ejecutará automáticamente al iniciar Windows" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Error al instalar el servicio: $_" -ForegroundColor Yellow
        Write-Host "   Puedes ejecutar el runner manualmente con: .\run.cmd" -ForegroundColor Yellow
    }
} else {
    Write-Host "⚠️  Paso 5: Saltado (requiere permisos de admin)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   Para ejecutar el runner manualmente:" -ForegroundColor Yellow
    Write-Host "   cd $RunnerDirectory" -ForegroundColor Cyan
    Write-Host "   .\run.cmd" -ForegroundColor Cyan
}

# Resumen final
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║          ✅ CONFIGURACIÓN COMPLETADA                  ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Próximos pasos:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Verifica que el runner aparece online en GitHub:" -ForegroundColor White
Write-Host "   https://github.com/Miguel-23-ing/ecommerce-microservice-backend-app/settings/actions/runners" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Ejecuta un workflow de prueba:" -ForegroundColor White
Write-Host "   - Ve a Actions en GitHub" -ForegroundColor Cyan
Write-Host "   - Selecciona 'Dev Environment - Build & Unit Tests'" -ForegroundColor Cyan
Write-Host "   - Click en 'Run workflow'" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Monitorea la ejecución:" -ForegroundColor White

if ($isAdmin) {
    Write-Host "   - El runner está ejecutándose como servicio" -ForegroundColor Cyan
    Write-Host "   - Puedes ver el estado con: .\svc.cmd status" -ForegroundColor Cyan
} else {
    Write-Host "   - Ejecuta manualmente: .\run.cmd" -ForegroundColor Cyan
    Write-Host "   - Mantén la ventana de PowerShell abierta" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "📚 Para más información, consulta:" -ForegroundColor White
Write-Host "   docs\GITHUB_RUNNER_SETUP.md" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎉 ¡Listo para ejecutar pipelines!" -ForegroundColor Green
Write-Host ""

# Mostrar comando para ejecutar manualmente si no es servicio
if (-not $isAdmin) {
    Write-Host "⚡ Para iniciar el runner ahora:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   cd $RunnerDirectory" -ForegroundColor Cyan
    Write-Host "   .\run.cmd" -ForegroundColor Cyan
    Write-Host ""
}
