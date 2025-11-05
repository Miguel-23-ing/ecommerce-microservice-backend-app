# 🚀 Pipelines CI/CD con GitHub Actions

## 📋 Descripción General

Este proyecto implementa 3 pipelines de CI/CD utilizando **GitHub Actions con Self-Hosted Runner** para automatizar el proceso de construcción, pruebas y despliegue de 6 microservicios:

- 👤 **user-service** - Gestión de usuarios
- 📦 **product-service** - Catálogo de productos
- 🛒 **order-service** - Procesamiento de pedidos
- 💳 **payment-service** - Procesamiento de pagos
- 🚚 **shipping-service** - Gestión de envíos
- ⭐ **favourite-service** - Favoritos de usuarios

## 🎯 Arquitectura de Pipelines

### Pipeline 1: 🔨 Dev Environment (`.github/workflows/dev-environment.yml`)

**Trigger:** Push/PR a ramas `dev` o `feature/**`

**Stages:**
1. **Build** - Compilación de cada microservicio con Maven
2. **Unit Tests** - Ejecución de 58 tests unitarios
3. **Package** - Generación de JARs
4. **Artifacts** - Almacenamiento de artifacts (7 días)

**Características:**
- ✅ Ejecución paralela (matriz de 6 servicios)
- ✅ Caché de Maven para mayor velocidad
- ✅ Reportes de tests automáticos
- ✅ Fail-fast deshabilitado (continúa si un servicio falla)

---

### Pipeline 2: 🚀 Stage Environment (`.github/workflows/stage-environment.yml`)

**Trigger:** Push a rama `stage` o ejecución manual

**Stages:**
1. **Build & Unit Tests** - Compilación + tests unitarios
2. **Integration Tests** - 47 tests de integración
3. **Docker Build** - Construcción de imágenes Docker
4. **Deploy to Minikube** - Despliegue en Kubernetes local
5. **Health Checks** - Validación de servicios

**Características:**
- ✅ Tests de integración entre servicios
- ✅ Validación de comunicación entre microservicios
- ✅ Despliegue automatizado a Kubernetes
- ✅ Verificación de health endpoints
- ✅ Artifacts almacenados por 14 días

---

### Pipeline 3: 🎯 Production Deployment (`.github/workflows/production-deployment.yml`)

**Trigger:** Push a `master/main` o ejecución manual con versión

**Stages:**
1. **Build & Unit Tests** - Build + 58 tests unitarios (fail-fast enabled)
2. **Integration Tests** - 47 tests de integración
3. **Deploy to Kubernetes** - Despliegue completo (infrastructure + microservices)
4. **E2E Tests** - Tests end-to-end de flujos completos
5. **Performance Tests** - Tests de carga con Locust
6. **Release Notes** - Generación automática de Release Notes
7. **Git Tag** - Creación y push de tag de versión

**Características:**
- ✅ Pipeline completo con todas las fases de testing
- ✅ Generación automática de Release Notes desde commits
- ✅ Versionado semántico con Git tags
- ✅ Tests de rendimiento y estrés
- ✅ Despliegue a producción (Kubernetes)
- ✅ Artifacts almacenados por 30 días
- ✅ Documentación automática de cambios

## 🛠️ Configuración del Entorno

### Requisitos del Self-Hosted Runner

- **OS:** Windows 10/11
- **RAM:** 16GB (para Minikube)
- **CPU:** 6 cores
- **Disk:** 50GB libres
- **Software:**
  - Java 11 (Eclipse Temurin)
  - Maven 3.8+
  - Docker Desktop
  - Minikube
  - Git
  - PowerShell 5.1+

### Variables de Entorno Configuradas

```yaml
JAVA_VERSION: '11'
MAVEN_OPTS: -Xmx2048m
MINIKUBE_CPUS: '6'
MINIKUBE_MEMORY: '16384'
```

## 📊 Cobertura de Pruebas

### Tests Unitarios (58 tests)
- ✅ Validación de componentes individuales
- ✅ Lógica de negocio aislada
- ✅ Mocks de dependencias externas
- ✅ Cobertura > 70%

**Servicios con tests unitarios:**
- user-service: 12 tests
- product-service: 10 tests
- order-service: 14 tests
- payment-service: 8 tests
- shipping-service: 8 tests
- favourite-service: 6 tests

### Tests de Integración (47 tests)
- ✅ Comunicación entre servicios
- ✅ Validación de APIs REST
- ✅ Integración con bases de datos
- ✅ Serialización/deserialización

**Servicios con tests de integración:**
- user-service: 15 tests
- product-service: 12 tests
- order-service: 20 tests

### Tests E2E (Flujos completos)
- ✅ Registro de usuario → Autenticación → Compra
- ✅ Navegación de catálogo → Agregar a carrito → Checkout
- ✅ Procesamiento de pago → Creación de envío → Notificación
- ✅ Gestión de favoritos → Compartir → Eliminar
- ✅ Búsqueda de productos → Filtros → Ordenamiento

### Tests de Rendimiento (Locust)
- ✅ Simulación de 1000 usuarios concurrentes
- ✅ Carga sostenida durante 5 minutos
- ✅ Métricas: p50, p90, p95, p99 response times
- ✅ Throughput: requests/second
- ✅ Tasa de errores

## 🚀 Ejecución de Pipelines

### Opción 1: Ejecución Manual (Recomendado para Testing)

1. Ve a **Actions** en GitHub
2. Selecciona el workflow deseado
3. Click en **Run workflow**
4. Configura parámetros (si aplica):
   - Para Production: `release_version` (ej: v1.0.0)
5. Click en **Run workflow**

### Opción 2: Ejecución Automática (por Push)

```bash
# Dev Pipeline
git checkout -b feature/nueva-funcionalidad
git add .
git commit -m "feat: nueva funcionalidad"
git push origin feature/nueva-funcionalidad

# Stage Pipeline
git checkout stage
git merge feature/nueva-funcionalidad
git push origin stage

# Production Pipeline
git checkout master
git merge stage
git push origin master
```

## 📝 Release Notes Automáticas

El pipeline de producción genera automáticamente Release Notes que incluyen:

- 📅 Fecha y hora del release
- 🏷️ Versión (versionado semántico)
- ✨ Lista de cambios desde el último tag
- 🧪 Resultados de tests
- 🚀 Información de despliegue
- 📊 Métricas de rendimiento
- 🔧 Cambios de configuración

**Formato de Release Notes:**

```markdown
# Release Notes - v1.0.0
**Release Date:** 2025-11-05 10:30:00
**Environment:** Production (Kubernetes)

## 📋 Summary
This release includes the deployment of 6 microservices...

## ✨ Changes
- feat: add user authentication (John Doe)
- fix: resolve payment processing bug (Jane Smith)
- perf: optimize database queries (Mike Johnson)

## 🧪 Test Results
- ✅ Unit Tests: Passed (58 tests)
- ✅ Integration Tests: Passed (47 tests)
- ✅ E2E Tests: Executed
- ✅ Performance Tests: Completed

## 🚀 Deployment
- Platform: Kubernetes (Minikube)
- Docker Images: Tagged with v1.0.0
- Services: 6 microservices deployed

## 📊 Metrics
- Response Time: < 200ms (p95)
- Throughput: 1000+ req/s
- Error Rate: < 0.1%
```

## 🏗️ Arquitectura de Despliegue

```
┌─────────────────────────────────────────────────────┐
│              Kubernetes (Minikube)                  │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐               │
│  │   Zipkin     │  │ Cloud Config │               │
│  │  (Tracing)   │  │   (Config)   │               │
│  └──────────────┘  └──────────────┘               │
│                                                     │
│  ┌──────────────┐  ┌──────────────┐               │
│  │   Eureka     │  │ API Gateway  │               │
│  │ (Discovery)  │  │  (Routing)   │               │
│  └──────────────┘  └──────────────┘               │
│                                                     │
│  ┌───────────────────────────────────────────┐    │
│  │         Microservices Layer               │    │
│  ├───────────────────────────────────────────┤    │
│  │ user-service    │ product-service         │    │
│  │ order-service   │ payment-service         │    │
│  │ shipping-service│ favourite-service       │    │
│  └───────────────────────────────────────────┘    │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## 📈 Métricas y Monitoreo

### Durante la Ejecución del Pipeline

- ⏱️ Tiempo de build por servicio
- 🧪 Cobertura de código
- ✅ Tests ejecutados/pasados/fallidos
- 📦 Tamaño de artifacts generados
- 🚀 Tiempo de despliegue

### Post-Despliegue

- 🏥 Health checks de todos los servicios
- 📊 Estado de pods en Kubernetes
- 🔍 Logs de aplicación
- 📈 Métricas de Zipkin (distributed tracing)

## 🔧 Troubleshooting

### Runner Offline
```powershell
cd C:\actions-runner
.\svc.cmd status
.\svc.cmd restart
```

### Build Falla - Out of Memory
```yaml
# Aumentar memoria de Maven en workflow
env:
  MAVEN_OPTS: -Xmx4096m
```

### Minikube No Inicia
```powershell
minikube delete
minikube start --driver=docker --cpus=6 --memory=16384
```

### Tests de Integración Fallan
- Verificar que servicios de soporte estén corriendo
- Verificar conexión a Eureka
- Revisar logs: `kubectl logs -n ecommerce-microservices -l app=<service>`

## 📚 Estructura de Archivos

```
.github/
└── workflows/
    ├── dev-environment.yml           # Pipeline DEV
    ├── stage-environment.yml         # Pipeline STAGE
    └── production-deployment.yml     # Pipeline PROD

docs/
├── GITHUB_RUNNER_SETUP.md           # Guía de setup del runner
└── PIPELINES_DOCUMENTATION.md       # Este archivo

setup-github-runner.ps1              # Script automatizado de setup
```

## 🎯 Mejores Prácticas Implementadas

✅ **Separation of Concerns** - Cada pipeline tiene un propósito específico  
✅ **Parallel Execution** - Builds paralelos para mayor velocidad  
✅ **Fail-Fast** - Detención temprana en producción si hay errores críticos  
✅ **Artifact Management** - Retención diferenciada según ambiente  
✅ **Automated Testing** - Tests en cada stage del pipeline  
✅ **Release Management** - Release Notes automáticas  
✅ **Change Management** - Versionado semántico con Git tags  
✅ **Observability** - Logs detallados y métricas  
✅ **Security** - Uso de self-hosted runner para proteger credenciales  

## 📞 Soporte

Para problemas o preguntas:
1. Revisa logs del workflow en GitHub Actions
2. Consulta `docs/GITHUB_RUNNER_SETUP.md`
3. Verifica logs del runner: `C:\actions-runner\_diag\`
4. Contacta al equipo de DevOps

---

**Última actualización:** 2025-11-05  
**Versión:** 1.0.0  
**Autor:** Miguel Angel
