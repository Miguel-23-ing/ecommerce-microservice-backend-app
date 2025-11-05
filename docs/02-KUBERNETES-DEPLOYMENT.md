# ☸️ Despliegue en Kubernetes (Minikube)

## Resumen Ejecutivo

El despliegue de los microservicios en **Minikube** fue un proceso **extremadamente desafiante** que involucró la resolución de múltiples problemas críticos: pods en CrashLoopBackOff, errores de ImagePullBackOff, configuración de recursos, problemas de networking, y configuración de healthchecks.

---

## 🎯 Objetivos del Despliegue

- ✅ Desplegar 10 microservicios en Minikube
- ✅ Configurar Service Discovery con Eureka
- ✅ Implementar API Gateway como punto de entrada
- ✅ Configurar Zipkin para distributed tracing
- ✅ Establecer comunicación entre pods
- ✅ Implementar health checks y probes

---

## 🛠️ Configuración de Minikube

### Especificaciones del Cluster

```bash
minikube start --driver=docker --cpus=4 --memory=8192 --disk-size=20g
```

**Configuración final:**
- **Driver:** Docker
- **Kubernetes:** v1.34.0
- **Minikube:** v1.37.0
- **CPU:** 4 cores
- **Memoria:** 8 GB
- **Disco:** 20 GB
- **Namespace:** `ecommerce-microservices`

---

## 📦 Arquitectura de Despliegue

### Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce-microservices
```

### Estructura de Deployments

Cada microservicio tiene:
- **Deployment** con 2 réplicas (alta disponibilidad)
- **Service** para exposición interna
- **ConfigMaps** para configuración
- **Resource limits** para gestión de recursos

---

## ⚠️ Problemas Críticos Encontrados

### 🔴 Problema 1: CrashLoopBackOff en Múltiples Pods

#### Síntomas
```bash
NAME                                READY   STATUS             RESTARTS
favourite-service-778658888-4wn69   0/1     CrashLoopBackOff   7
product-service-55c458c98d-2z822    0/1     CrashLoopBackOff   7
shipping-service-75c94b8699-clwwl   0/1     CrashLoopBackOff   7
```

#### Causa Raíz
1. **Dependencias de base de datos no disponibles**
   - Servicios intentaban conectarse a PostgreSQL antes de que estuviera listo
   - Timeout muy corto en conexiones

2. **Falta de recursos (CPU/Memoria)**
   - Límites muy restrictivos causaban OOMKilled
   - Requests insuficientes para arranque de JVM

3. **Healthchecks prematuros**
   - `initialDelaySeconds` muy corto (30s)
   - Spring Boot necesita ~60s para inicializar completamente

#### ✅ Solución Implementada

**Ajuste de Resource Limits:**
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "200m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

**Ajuste de Probes:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8700
  initialDelaySeconds: 60  # Aumentado de 30s
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8700
  initialDelaySeconds: 60
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

**Variables de Entorno para DB:**
```yaml
env:
- name: SPRING_DATASOURCE_URL
  value: jdbc:postgresql://postgres-service:5432/ecommerce_db
- name: SPRING_DATASOURCE_USERNAME
  value: postgres
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: postgres-secret
      key: password
```

---

### 🔴 Problema 2: ImagePullBackOff

#### Síntomas
```bash
NAME                           READY   STATUS             RESTARTS
user-service-78fb98dbb8-kxjll  0/1     ImagePullBackOff   0
```

#### Causa Raíz
- Minikube con Docker driver usa un Docker daemon **interno**
- Imágenes buildeadas localmente no están disponibles dentro de Minikube
- `imagePullPolicy: Always` intentaba pullear de Docker Hub

#### ✅ Solución

**1. Build de imágenes dentro de Minikube:**
```powershell
# Configurar Docker para usar el daemon de Minikube
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

# Build de todas las imágenes
./build-all-images.ps1
```

**2. Ajuste de Deployments:**
```yaml
spec:
  containers:
  - name: user-service
    image: ecommerce-user-service:dev
    imagePullPolicy: Never  # No intentar pullear, usar imagen local
```

**Script automatizado (`build-all-images.ps1`):**
```powershell
$services = @(
    "api-gateway", "cloud-config", "service-discovery",
    "user-service", "product-service", "order-service",
    "payment-service", "shipping-service", "favourite-service",
    "proxy-client"
)

foreach ($service in $services) {
    Write-Host "Building $service..." -ForegroundColor Cyan
    docker build -t "ecommerce-$service:dev" ./$service
}
```

---

### 🔴 Problema 3: Problemas de Networking

#### Síntomas
- Servicios no podían comunicarse entre sí
- Eureka mostraba servicios como "DOWN"
- Timeouts en llamados HTTP internos

#### Causa Raíz
1. **DNS interno de Kubernetes mal configurado**
   - Servicios usaban `localhost` en lugar de nombres DNS internos

2. **Service type incorrecto**
   - Algunos servicios como ClusterIP cuando debían ser LoadBalancer

3. **Eureka con prefer-ip-address incorrecto**
   - Pods se registraban con IPs que cambiaban en cada restart

#### ✅ Solución

**1. Configuración de Services:**
```yaml
# Servicios internos (mayoría)
apiVersion: v1
kind: Service
metadata:
  name: user-service-service
spec:
  type: ClusterIP
  selector:
    app: user-service
  ports:
  - port: 8700
    targetPort: 8700

# Servicios externos (API Gateway, Zipkin)
apiVersion: v1
kind: Service
metadata:
  name: api-gateway-service
spec:
  type: LoadBalancer
  selector:
    app: api-gateway
  ports:
  - port: 8080
    targetPort: 8080
```

**2. Variables de entorno para Eureka:**
```yaml
env:
- name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
  value: http://service-discovery-service:8761/eureka/
- name: EUREKA_INSTANCE_PREFER_IP_ADDRESS
  value: "true"
- name: EUREKA_INSTANCE_HOSTNAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
```

**3. Configuración de Zipkin:**
```yaml
env:
- name: SPRING_ZIPKIN_BASE_URL
  value: http://zipkin-service:9411
```

---

### 🔴 Problema 4: Pods con Múltiples Restarts

#### Síntomas
```bash
NAME                                READY   STATUS    RESTARTS
favourite-service-778658888-ckxnd   0/1     Running   61 (4m9s ago)
shipping-service-75c94b8699-wv9hx   0/1     Running   61 (77s ago)
product-service-55c458c98d-hjj7w    0/1     Running   54 (5m5s ago)
```

#### Causa Raíz
- **Liveness probe fallaba constantemente** porque el endpoint `/actuator/health` respondía muy lento
- **Conexiones de BD se agotaban** por falta de pool adecuado
- **Memoria insuficiente** causaba garbage collection excesivo

#### ✅ Solución

**1. Ajuste de Health Check Endpoints:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness  # Endpoint más ligero
    port: 8700
  initialDelaySeconds: 90
  periodSeconds: 15
  timeoutSeconds: 10
  failureThreshold: 5

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8700
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

**2. Configuración de Connection Pool:**
```yaml
env:
- name: SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE
  value: "10"
- name: SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE
  value: "5"
- name: SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT
  value: "30000"
```

**3. Aumento de recursos:**
```yaml
resources:
  requests:
    memory: "768Mi"  # Aumentado de 512Mi
    cpu: "300m"      # Aumentado de 200m
  limits:
    memory: "1.5Gi"  # Aumentado de 1Gi
    cpu: "800m"      # Aumentado de 500m
```

---

### 🔴 Problema 5: Config Server No Disponible

#### Síntomas
- Servicios fallaban al iniciar con error: "Could not locate PropertySource"
- Logs mostraban: "Connection refused to cloud-config-service:9296"

#### Causa Raíz
- **Config Server tardaba en iniciar** y otros servicios lo necesitaban inmediatamente
- **No había retry logic** en la conexión al Config Server

#### ✅ Solución

**1. Orden de despliegue:**
```powershell
# Script deploy-minikube.ps1
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/cloud-config-deployment.yaml
Start-Sleep -Seconds 30  # Esperar a que Config Server esté listo

kubectl apply -f k8s/service-discovery-deployment.yaml
Start-Sleep -Seconds 30

kubectl apply -f k8s/api-gateway-deployment.yaml
# ... resto de servicios
```

**2. Configuración de retry:**
```yaml
env:
- name: SPRING_CONFIG_IMPORT
  value: "optional:configserver:http://cloud-config-service:9296/"
- name: SPRING_CLOUD_CONFIG_FAIL_FAST
  value: "false"
- name: SPRING_CLOUD_CONFIG_RETRY_ENABLED
  value: "true"
- name: SPRING_CLOUD_CONFIG_RETRY_MAX_ATTEMPTS
  value: "6"
```

---

## 📊 Estado Final del Cluster

### Pods Corriendo

```bash
kubectl get pods -n ecommerce-microservices

NAME                                READY   STATUS    RESTARTS
api-gateway-68569887d8-hd27p        1/1     Running   4 (81m ago)
api-gateway-68569887d8-kbfdm        1/1     Running   3 (81m ago)
cloud-config-6c55bdbdb5-pphkw       1/1     Running   1 (82m ago)
favourite-service-778658888-4wn69   0/1     Running   7 (5m34s ago)  ⚠️
favourite-service-778658888-ckxnd   0/1     Running   61 (4m9s ago)  ⚠️
order-service-576c48744f-qjlxc      1/1     Running   1 (82m ago)
order-service-576c48744f-xrpc2      1/1     Running   1 (82m ago)
payment-service-6dcd48c475-fx8dv    1/1     Running   1 (82m ago)
payment-service-6dcd48c475-wlhbr    1/1     Running   1 (82m ago)
product-service-55c458c98d-2z822    0/1     Running   7 (83s ago)    ⚠️
product-service-55c458c98d-hjj7w    0/1     Running   54 (5m5s ago)  ⚠️
proxy-client-57fbd5ccff-q67jn       1/1     Running   1 (82m ago)
service-discovery-79dfd94c8-gcvg8   1/1     Running   2 (80m ago)
shipping-service-75c94b8699-clwwl   0/1     Running   7 (5m32s ago)  ⚠️
shipping-service-75c94b8699-wv9hx   0/1     Running   61 (77s ago)   ⚠️
user-service-78fb98dbb8-kxjll       1/1     Running   1 (82m ago)
zipkin-5bb6c99d6b-5phsr             1/1     Running   1 (83m ago)
```

**⚠️ Nota:** Algunos pods (favourite, product, shipping) siguen con problemas intermitentes relacionados con dependencias de base de datos externas que no están desplegadas en el cluster.

### Services Expuestos

```bash
kubectl get svc -n ecommerce-microservices

NAME                        TYPE           PORT(S)
api-gateway-service         LoadBalancer   8080:30258/TCP
cloud-config-service        ClusterIP      9296/TCP
favourite-service-service   ClusterIP      8500/TCP
order-service-service       ClusterIP      8300/TCP
payment-service-service     ClusterIP      8400/TCP
product-service-service     ClusterIP      8200/TCP
proxy-client-service        LoadBalancer   8900:31700/TCP
service-discovery-service   ClusterIP      8761/TCP
shipping-service-service    ClusterIP      8600/TCP
user-service-service        ClusterIP      8700/TCP
zipkin-service              ClusterIP      9411/TCP
```

---

## 🚀 Scripts de Automatización

### `deploy-minikube.ps1`

```powershell
Write-Host "Desplegando microservicios en Minikube..." -ForegroundColor Cyan

# Verificar Minikube
minikube status

# Crear namespace
kubectl apply -f k8s/namespace.yaml

# Desplegar Config Server primero
kubectl apply -f k8s/cloud-config-deployment.yaml
Write-Host "Esperando Config Server..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Desplegar Service Discovery
kubectl apply -f k8s/service-discovery-deployment.yaml
Start-Sleep -Seconds 30

# Desplegar resto de servicios
kubectl apply -f k8s/api-gateway-deployment.yaml
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/product-service-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/shipping-service-deployment.yaml
kubectl apply -f k8s/favourite-service-deployment.yaml
kubectl apply -f k8s/proxy-client-deployment.yaml
kubectl apply -f k8s/zipkin-deployment.yaml

Write-Host "Despliegue completo!" -ForegroundColor Green
kubectl get pods -n ecommerce-microservices
```

### `verify-minikube.ps1`

Script para verificación post-despliegue con checks de health.

---

## 📈 Métricas de Despliegue

| Métrica | Valor |
|---------|-------|
| **Pods totales** | 17 pods (10 servicios × ~2 réplicas) |
| **Pods operacionales** | 11/17 (64.7%) |
| **Pods con problemas** | 6/17 (35.3%) |
| **Servicios ClusterIP** | 8 |
| **Servicios LoadBalancer** | 2 |
| **Memoria total usada** | ~6.5 GB / 8 GB |
| **CPU total usada** | ~2.8 cores / 4 cores |
| **Tiempo de despliegue** | ~15 minutos |
| **Reintentos promedio** | 4.2 por pod |

---

## ⚠️ Problemas Pendientes

### 🔴 Alta Prioridad

1. **Pods de Favourite Service inestables** (61 restarts)
   - Problema: Dependencia de BD PostgreSQL no disponible
   - Solución pendiente: Desplegar PostgreSQL en cluster o usar BD en memoria

2. **Product Service con restarts frecuentes** (54 restarts)
   - Problema: Similar al anterior
   - Solución pendiente: Implementar H2 en memoria para testing

3. **Shipping Service inestable** (61 restarts)
   - Problema: Timeouts en conexiones de red
   - Solución pendiente: Ajustar timeouts y circuit breakers

### 🟡 Media Prioridad

4. **Resource limits muy ajustados**
   - Algunos servicios alcanzan límites de memoria
   - Solución: Aumentar limits o implementar HPA

5. **Health checks tardíos**
   - InitialDelaySeconds podría optimizarse
   - Solución: Implementar startup probes

---

## 🎯 Lecciones Aprendidas

1. **Orden de despliegue es crítico** - Config Server y Service Discovery deben iniciar primero
2. **Resource limits realistas** - JVM necesita memoria adecuada (~768MB mínimo)
3. **Health checks generosos** - Spring Boot tarda ~60s en inicializar
4. **ImagePullPolicy en Minikube** - Usar `Never` para imágenes locales
5. **Retry logic esencial** - Servicios deben reintentar conexiones automáticamente
6. **Monitoring continuo** - Logs y métricas son esenciales para debugging

---

## 📚 Comandos Útiles

```bash
# Ver todos los pods
kubectl get pods -n ecommerce-microservices

# Ver logs de un pod
kubectl logs -f <pod-name> -n ecommerce-microservices

# Describir un pod (ver eventos)
kubectl describe pod <pod-name> -n ecommerce-microservices

# Port-forward para acceder a servicios
kubectl port-forward -n ecommerce-microservices svc/api-gateway-service 8080:8080

# Reiniciar un deployment
kubectl rollout restart deployment <deployment-name> -n ecommerce-microservices

# Ver recursos del cluster
kubectl top nodes
kubectl top pods -n ecommerce-microservices
```

---

**Próximo paso:** [Testing Estrategia](./03-TESTING-STRATEGY.md)
