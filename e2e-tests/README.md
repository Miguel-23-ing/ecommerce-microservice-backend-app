# End-to-End Tests (E2E)

## 📋 Descripción

Este módulo contiene **5 pruebas E2E completas** que validan flujos de usuario end-to-end a través de múltiples microservicios del sistema de e-commerce.

## 🎯 Flujos de Usuario Validados

### 1️⃣ **User Registration Flow** (`UserRegistrationFlowE2ETest`)
**Flujo**: Registro completo de usuario y gestión de perfil
- ✅ Crear nuevo usuario con credenciales
- ✅ Verificar que el usuario existe
- ✅ Actualizar información del usuario
- ✅ Consultar perfil actualizado
- ✅ Listar todos los usuarios

**Microservicios involucrados**: User Service

---

### 2️⃣ **Shopping and Favorites Flow** (`ShoppingAndFavoritesFlowE2ETest`)
**Flujo**: Navegación de productos y gestión de favoritos
- ✅ Crear usuario comprador
- ✅ Buscar y listar productos disponibles
- ✅ Consultar detalles de producto específico
- ✅ Agregar producto a favoritos
- ✅ Verificar que el favorito existe
- ✅ Listar favoritos del usuario

**Microservicios involucrados**: User Service, Product Service, Favourite Service

---

### 3️⃣ **Order Creation and Processing Flow** (`OrderCreationAndProcessingFlowE2ETest`)
**Flujo**: Creación y procesamiento de órdenes de compra
- ✅ Crear usuario comprador
- ✅ Seleccionar producto para comprar
- ✅ Crear orden de compra
- ✅ Verificar creación de orden
- ✅ Actualizar estado de la orden
- ✅ Consultar historial de órdenes

**Microservicios involucrados**: User Service, Product Service, Order Service

---

### 4️⃣ **Payment Processing Flow** (`PaymentProcessingFlowE2ETest`)
**Flujo**: Procesamiento completo de pagos
- ✅ Crear usuario pagador
- ✅ Crear orden de compra
- ✅ Procesar pago de la orden
- ✅ Verificar registro del pago
- ✅ Actualizar estado del pago
- ✅ Listar todos los pagos

**Microservicios involucrados**: User Service, Order Service, Payment Service

---

### 5️⃣ **Shipping and Fulfillment Flow** (`ShippingAndFulfillmentFlowE2ETest`)
**Flujo**: Envío completo desde orden hasta tracking
- ✅ Crear usuario cliente
- ✅ Seleccionar producto
- ✅ Crear orden de compra
- ✅ Procesar pago
- ✅ Crear registro de envío
- ✅ Actualizar cantidad en envío
- ✅ Consultar historial de envíos
- ✅ Verificar tracking completo (Orden → Pago → Envío)

**Microservicios involucrados**: User Service, Product Service, Order Service, Payment Service, Shipping Service

---

## 🛠️ Tecnologías Utilizadas

- **JUnit 5**: Framework de testing
- **REST Assured**: Testing de APIs REST
- **Awaitility**: Aserciones asíncronas y eventual consistency
- **Spring Boot Test**: Contexto de pruebas Spring
- **Lombok**: Reducción de boilerplate

## 🚀 Ejecución de Tests

### Ejecutar todos los tests E2E:
```bash
mvn test -pl e2e-tests
```

### Ejecutar un test específico:
```bash
mvn test -pl e2e-tests -Dtest=UserRegistrationFlowE2ETest
```

### Ejecutar con perfil específico:
```bash
mvn test -pl e2e-tests -Dspring.profiles.active=test
```

## ⚙️ Configuración

Los tests requieren que los servicios estén corriendo. Por defecto se conectan a:
- **API Gateway**: `http://localhost:8080`

Para cambiar la URL base:
```bash
mvn test -pl e2e-tests -Dapi.gateway.url=http://localhost:8080
```

## 📊 Características de los Tests

### ✅ Ordenación de Tests
Todos los tests usan `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` para garantizar ejecución secuencial del flujo.

### ✅ Cleanup Automático
Cada test incluye un método `@AfterAll` que limpia los datos creados durante la prueba.

### ✅ Eventual Consistency
Los tests usan `Awaitility` para manejar delays de consistencia eventual entre microservicios.

### ✅ Logs Descriptivos
Cada paso del flujo imprime logs con emojis para facilitar el seguimiento:
- ✅ Operación exitosa
- 🧹 Cleanup de datos
- ⚠️ Advertencia

### ✅ Aserciones Robustas
- Validación de códigos HTTP (200, 201, 202)
- Verificación de respuesta JSON completa
- Assertions de datos críticos

## 📝 Estructura de un Test E2E

```java
@Tag("e2e")                                      // Tag para agrupar tests
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)  // Orden secuencial
@ActiveProfiles("test")                         // Perfil de test
class MyE2ETest {
    
    @BeforeAll
    static void setUp() {
        // Configuración inicial
    }
    
    @Test
    @Order(1)
    @DisplayName("E2E-X.1: Descripción del paso")
    void shouldDoFirstStep() {
        // Implementación del paso
    }
    
    @AfterAll
    static void cleanup() {
        // Limpieza de datos
    }
}
```

## 🎯 Métricas de Cobertura E2E

| Flujo | Servicios | Steps | Assertions |
|-------|-----------|-------|------------|
| User Registration | 1 | 5 | 15+ |
| Shopping & Favorites | 3 | 6 | 18+ |
| Order Processing | 3 | 6 | 18+ |
| Payment Processing | 3 | 6 | 18+ |
| Shipping & Fulfillment | 5 | 9 | 27+ |
| **TOTAL** | **5 servicios** | **32 steps** | **96+ assertions** |

## 🔍 Casos de Uso Validados

- ✅ **CRUD completo** de usuarios, productos, órdenes, pagos, envíos
- ✅ **Integración entre servicios** (user → product → order → payment → shipping)
- ✅ **Flujos de negocio reales** (compra, pago, envío)
- ✅ **Validación de estados** (creación, actualización, consulta)
- ✅ **Gestión de favoritos** (agregar, listar, verificar)

## 🐛 Troubleshooting

### Los tests fallan con "Connection refused"
**Solución**: Asegúrate de que todos los servicios estén corriendo:
```bash
docker-compose up -d
# o
kubectl get pods -n ecommerce
```

### Tests timeout con Awaitility
**Solución**: Aumenta el timeout en `application-test.properties`:
```properties
e2e.timeout.seconds=60
```

### Datos no se limpian correctamente
**Solución**: Los tests incluyen cleanup automático en `@AfterAll`. Si persiste, limpia manualmente:
```bash
# Eliminar datos de test
curl -X DELETE http://localhost:8080/api/users/{userId}
```

## 📈 Próximos Pasos

- [ ] Agregar tests de performance (load testing)
- [ ] Implementar tests de seguridad (authentication/authorization)
- [ ] Agregar tests de resiliencia (circuit breaker, retry)
- [ ] Implementar tests de chaos engineering
- [ ] Agregar reportes visuales de cobertura E2E

---

## 📄 Licencia

Este módulo es parte del proyecto **ecommerce-microservice-backend-app**.
