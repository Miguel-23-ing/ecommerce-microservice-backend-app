# 🚀 Configuración de GitHub Actions Self-Hosted Runner

## 📋 Requisitos Previos

- Windows 10/11
- PowerShell 5.1 o superior
- Docker Desktop instalado y corriendo
- Minikube instalado
- Git instalado
- Cuenta de GitHub con permisos de administrador en el repositorio

## 🔧 Paso 1: Configurar el Runner en GitHub

### 1.1 Acceder a Settings del Repositorio

1. Ve a tu repositorio en GitHub: `https://github.com/Miguel-23-ing/ecommerce-microservice-backend-app`
2. Click en **Settings** (⚙️)
3. En el menú lateral, click en **Actions** → **Runners**
4. Click en **New self-hosted runner**

### 1.2 Seleccionar Sistema Operativo

- Operating System: **Windows**
- Architecture: **x64**

## 📥 Paso 2: Descargar e Instalar el Runner

### 2.1 Crear Directorio para el Runner

Abre PowerShell como **Administrador** y ejecuta:

```powershell
# Crear directorio
mkdir C:\actions-runner
cd C:\actions-runner

# Descargar el runner (versión actualizada)
Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip -OutFile actions-runner-win-x64-2.311.0.zip

# Extraer
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD\actions-runner-win-x64-2.311.0.zip", "$PWD")
```

### 2.2 Configurar el Runner

Ejecuta el script de configuración con el token que te proporciona GitHub:

```powershell
# Reemplaza [TOKEN] con el token que GitHub te muestra
./config.cmd --url https://github.com/Miguel-23-ing/ecommerce-microservice-backend-app --token [TOKEN]
```

**Durante la configuración, responde:**

- Enter the name of the runner group: `[Enter]` (default)
- Enter the name of runner: `windows-local-runner` (o el nombre que prefieras)
- Enter any additional labels: `self-hosted,Windows,X64,java,docker,kubernetes` 
- Enter name of work folder: `[Enter]` (default: _work)

### 2.3 Instalar el Runner como Servicio (Opcional pero Recomendado)

Para que el runner se ejecute automáticamente al iniciar Windows:

```powershell
# Instalar como servicio
./svc.cmd install

# Iniciar el servicio
./svc.cmd start

# Verificar estado
./svc.cmd status
```

## ▶️ Paso 3: Ejecutar el Runner Manualmente (Alternativa)

Si prefieres ejecutar el runner manualmente sin instalarlo como servicio:

```powershell
cd C:\actions-runner
./run.cmd
```

**IMPORTANTE:** Mantén esta ventana de PowerShell abierta mientras ejecutas los workflows.

## ✅ Paso 4: Verificar la Configuración

### 4.1 Verificar en GitHub

1. Ve a Settings → Actions → Runners
2. Deberías ver tu runner con estado **Idle** (verde) 🟢

### 4.2 Verificar Herramientas Necesarias

El runner necesita acceso a estas herramientas. Verifica que estén en el PATH:

```powershell
# Verificar Java
java -version

# Verificar Docker
docker --version

# Verificar Minikube
minikube version

# Verificar Maven (dentro de cada proyecto)
cd user-service
./mvnw.cmd --version
```

## 🚀 Paso 5: Ejecutar los Workflows

### 5.1 Workflow Manual (Recomendado para Probar)

1. Ve a **Actions** en GitHub
2. Selecciona el workflow que quieres ejecutar:
   - `🔨 Dev Environment - Build & Unit Tests`
   - `🚀 Stage Environment - Integration Tests & Deploy`
   - `🎯 Production Deployment - Full Pipeline`
3. Click en **Run workflow**
4. Selecciona la rama (master/main)
5. Para Production, ingresa la versión (ej: `v1.0.0`)
6. Click en **Run workflow**

### 5.2 Workflow Automático (con Push)

Para que los workflows se ejecuten automáticamente:

```powershell
# Crear rama dev para testing
git checkout -b dev
git push origin dev

# Crear rama stage
git checkout -b stage  
git push origin stage

# Push a master activa el workflow de producción
git checkout master
git push origin master
```

## 📊 Paso 6: Monitorear la Ejecución

### 6.1 En GitHub

- Ve a **Actions** en el repositorio
- Verás todos los workflows ejecutándose/completados
- Click en cualquier workflow para ver logs detallados

### 6.2 En PowerShell (Runner Local)

Si ejecutas el runner manualmente, verás los logs en tiempo real:

```
√ Connected to GitHub
2025-11-05 10:00:00Z: Listening for Jobs
2025-11-05 10:01:15Z: Running job: Build user-service
...
```

## 🛠️ Solución de Problemas

### Runner No Aparece Online

```powershell
# Verificar que Docker está corriendo
docker ps

# Reiniciar el servicio del runner
cd C:\actions-runner
./svc.cmd stop
./svc.cmd start
```

### Error: "Unable to connect to GitHub"

1. Verificar conexión a internet
2. Verificar firewall/proxy
3. Regenerar token en GitHub Settings → Runners → New runner

### Error: "Java/Maven not found"

Asegúrate que las herramientas estén en el PATH del sistema:

```powershell
# Verificar PATH
$env:PATH -split ';' | Where-Object { $_ -match 'java|maven' }

# Agregar Java al PATH (si es necesario)
$env:PATH += ";C:\Program Files\Eclipse Adoptium\jdk-11.0.XX-hotspot\bin"
```

### Workflow Falla: "No space left on device"

Limpiar imágenes Docker antiguas:

```powershell
# Limpiar imágenes no usadas
docker system prune -a -f

# Limpiar volúmenes
docker volume prune -f
```

## 📝 Comandos Útiles

```powershell
# Ver estado del runner
cd C:\actions-runner
./svc.cmd status

# Ver logs del runner
Get-Content C:\actions-runner\_diag\Runner_*.log -Tail 50

# Detener runner temporalmente
./svc.cmd stop

# Desinstalar runner
./svc.cmd uninstall
./config.cmd remove --token [NEW_TOKEN]
```

## 🎯 Siguiente Paso

Una vez configurado el runner:

1. ✅ Runner instalado y corriendo
2. 🚀 Ejecutar workflow de DEV manualmente
3. 📊 Capturar pantallazos de la ejecución
4. 📝 Documentar resultados para el informe

## 📚 Referencias

- [GitHub Actions Self-Hosted Runners Documentation](https://docs.github.com/en/actions/hosting-your-own-runners)
- [Configuring the self-hosted runner application as a service](https://docs.github.com/en/actions/hosting-your-own-runners/configuring-the-self-hosted-runner-application-as-a-service)
