# Despliegue en Minikube - E-Commerce Microservices

## 📋 Prerequisitos

- Docker Desktop instalado y corriendo
- Minikube instalado
- kubectl instalado
- Maven instalado (para compilar los JARs)

## 🚀 Despliegue Automático

### Opción 1: Script Completo (Recomendado)

```powershell
.\deploy-minikube.ps1
```

Este script realiza automáticamente:
1. Verifica que Minikube esté corriendo (lo inicia si es necesario)
2. Configura Docker para usar el daemon de Minikube
3. Compila los JARs con Maven
4. Construye las imágenes Docker dentro de Minikube
5. Crea el namespace `ecommerce-microservices`
6. Despliega los servicios de infraestructura (Zipkin, Cloud Config, Service Discovery)
7. Despliega los microservicios de negocio
8. Muestra el estado de los pods

### Opción 2: Paso a Paso

Si prefieres ejecutar los pasos manualmente:

```powershell
# 1. Iniciar Minikube
minikube start --driver=docker --cpus=4 --memory=8192

# 2. Configurar Docker para Minikube
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

# 3. Compilar JARs
.\mvnw clean package -DskipTests

# 4. Construir imágenes Docker
docker-compose build

# 5. Verificar imágenes (deben aparecer en Minikube)
docker images | findstr ecommerce

# 6. Crear namespace
kubectl apply -f k8s/namespace.yaml

# 7. Desplegar servicios de infraestructura
kubectl apply -f k8s/zipkin-deployment.yaml
kubectl apply -f k8s/cloud-config-deployment.yaml
kubectl apply -f k8s/service-discovery-deployment.yaml

# Esperar 30 segundos
Start-Sleep -Seconds 30

# 8. Desplegar microservicios
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/product-service-deployment.yaml
kubectl apply -f k8s/favourite-service-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/shipping-service-deployment.yaml
kubectl apply -f k8s/api-gateway-deployment.yaml
```

## 🔍 Verificación del Despliegue

```powershell
# Verificar estado de pods, servicios y deployments
.\verify-minikube.ps1

# O manualmente:
kubectl get pods -n ecommerce-microservices
kubectl get services -n ecommerce-microservices
kubectl get deployments -n ecommerce-microservices
```

## 🌐 Acceso a los Servicios

Para acceder a los servicios desde tu máquina local, usa port-forward:

### API Gateway (Puerto principal de entrada)
```powershell
kubectl port-forward service/api-gateway-service 8080:8080 -n ecommerce-microservices
```
Luego accede a: `http://localhost:8080`

### Service Discovery (Eureka)
```powershell
kubectl port-forward service/service-discovery-service 8761:8761 -n ecommerce-microservices
```
Dashboard: `http://localhost:8761`

### Zipkin (Distributed Tracing)
```powershell
kubectl port-forward service/zipkin-service 9411:9411 -n ecommerce-microservices
```
Dashboard: `http://localhost:9411`

### Servicios Individuales

Si necesitas acceder a un microservicio específico:

```powershell
# User Service (8700)
kubectl port-forward service/user-service-service 8700:8700 -n ecommerce-microservices

# Product Service (8200)
kubectl port-forward service/product-service-service 8200:8200 -n ecommerce-microservices

# Order Service (8300)
kubectl port-forward service/order-service-service 8300:8300 -n ecommerce-microservices

# Payment Service (8400)
kubectl port-forward service/payment-service-service 8400:8400 -n ecommerce-microservices

# Favourite Service (8500)
kubectl port-forward service/favourite-service-service 8500:8500 -n ecommerce-microservices

# Shipping Service (8600)
kubectl port-forward service/shipping-service-service 8600:8600 -n ecommerce-microservices
```

## 📊 Monitoreo y Logs

### Ver logs de un pod específico
```powershell
kubectl logs <pod-name> -n ecommerce-microservices -f
```

### Ver logs de todos los pods de un servicio
```powershell
kubectl logs -l app=user-service -n ecommerce-microservices --tail=100
```

### Describir un pod (útil para debugging)
```powershell
kubectl describe pod <pod-name> -n ecommerce-microservices
```

### Ver eventos del namespace
```powershell
kubectl get events -n ecommerce-microservices --sort-by='.lastTimestamp'
```

## 🔄 Actualizar Servicios

Si realizas cambios en el código:

```powershell
# 1. Recompilar JARs
.\mvnw clean package -DskipTests

# 2. Reconstruir imágenes
docker-compose build

# 3. Reiniciar deployments (usa las nuevas imágenes)
kubectl rollout restart deployment/<deployment-name> -n ecommerce-microservices

# O reiniciar todos:
kubectl rollout restart deployment -n ecommerce-microservices
```

## 🧹 Limpieza

### Eliminar todos los recursos
```powershell
kubectl delete namespace ecommerce-microservices
```

### Detener Minikube
```powershell
minikube stop
```

### Eliminar Minikube completamente
```powershell
minikube delete
```

## 🐛 Troubleshooting

### Los pods están en estado "Pending" o "ImagePullBackOff"
- Verifica que las imágenes existan en Minikube: `docker images`
- Asegúrate de estar usando el daemon de Docker de Minikube
- Revisa que `imagePullPolicy: Never` esté configurado en los deployments

### Los pods están en "CrashLoopBackOff"
- Revisa los logs: `kubectl logs <pod-name> -n ecommerce-microservices`
- Verifica las health checks (startupProbe, livenessProbe, readinessProbe)
- Aumenta el `failureThreshold` o `initialDelaySeconds` si los servicios tardan en iniciar

### Servicios no se pueden comunicar entre sí
- Verifica que Service Discovery (Eureka) esté corriendo
- Revisa los logs de Eureka para ver qué servicios están registrados
- Confirma que las variables de entorno `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` estén correctas

### Errores de base de datos
- Los servicios están configurados para usar H2 in-memory por defecto
- Si necesitas persistencia, modifica los deployments para usar MySQL/PostgreSQL con PersistentVolumes

## 📝 Notas

- Los servicios usan bases de datos H2 in-memory (sin persistencia)
- Cloud Config está en modo `native` (lee archivos locales)
- Todos los servicios están en el namespace `ecommerce-microservices`
- Se configuraron health checks con tiempos de espera generosos para el arranque inicial

## 🎯 Próximos Pasos

Una vez desplegado en Minikube, puedes:

1. ✅ Ejecutar tests E2E contra los servicios en Minikube
2. ✅ Configurar Ingress para acceso externo más elegante
3. ✅ Agregar Persistent Volumes para bases de datos reales
4. ✅ Implementar HorizontalPodAutoscaler para escalado automático
5. ✅ Configurar ConfigMaps y Secrets para configuración externa
