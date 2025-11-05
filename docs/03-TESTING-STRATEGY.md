# 🧪 Estrategia de Testing

## Resumen Ejecutivo

Se implementó una **estrategia de testing completa** con tres niveles: **Unit Tests** (56 tests), **Integration Tests** (45 tests), y **E2E Tests** (5 flujos). Los tests unitarios y de integración están **funcionando al 100%**, mientras que los E2E tests presentan **problemas de conectividad** con los servicios en Minikube.

---

## 🎯 Objetivos de Testing

- ✅ Implementar tests unitarios para lógica de negocio
- ✅ Implementar tests de integración para APIs
- ⚠️ Implementar tests E2E para flujos completos (parcial)
- ✅ Lograr >75% de cobertura de código
- ✅ Automatizar ejecución de tests en CI/CD

---

## 📊 Resumen de Tests

| Tipo | Cantidad | Estado | Cobertura |
|------|----------|--------|-----------|
| **Unit Tests** | 56 | ✅ 100% passing | 78% |
| **Integration Tests** | 45 | ✅ 100% passing | 65% |
| **E2E Tests** | 5 | ⚠️ No funcionales | 0% |
| **Total** | 106 | ✅ 95.3% passing | 71% |

---

## 🔬 Unit Tests

### Descripción

Tests aislados que verifican la **lógica de negocio** de cada componente sin dependencias externas. Se usan **mocks** para servicios externos.

### Tecnologías

- **JUnit 5** - Framework de testing
- **Mockito** - Mocking de dependencias
- **AssertJ** - Assertions fluidas
- **Spring Boot Test** - Testing utilities

### Distribución por Servicio

| Servicio | Unit Tests | Estado |
|----------|-----------|--------|
| User Service | 12 | ✅ Passing |
| Product Service | 10 | ✅ Passing |
| Order Service | 9 | ✅ Passing |
| Payment Service | 8 | ✅ Passing |
| Shipping Service | 9 | ✅ Passing |
| Favourite Service | 8 | ✅ Passing |
| **Total** | **56** | **✅ 100%** |

### Ejemplo: User Service Test

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser() {
        // Given
        UserDto userDto = UserDto.builder()
            .username("testuser")
            .email("test@example.com")
            .build();
            
        User savedUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .build();
            
        when(userRepository.save(any(User.class)))
            .thenReturn(savedUser);
        
        // When
        UserDto result = userService.createUser(userDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetUserNotFound() {
        // Given
        when(userRepository.findById(999L))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("User not found with id: 999");
    }
}
```

### Comandos de Ejecución

```powershell
# Ejecutar unit tests de un servicio
cd user-service
.\mvnw.cmd test -DskipITs=true

# Ejecutar todos los unit tests
.\mvnw.cmd clean test -DskipITs=true
```

### Resultados

```
[INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 🔗 Integration Tests

### Descripción

Tests que verifican la **integración entre componentes** y la funcionalidad de **APIs REST**. Usan base de datos en memoria (H2) y Spring MockMvc.

### Tecnologías

- **Spring Boot Test** - Testing framework
- **MockMvc** - Testing de controllers
- **H2 Database** - BD en memoria
- **TestRestTemplate** - Cliente HTTP de testing
- **Testcontainers** - Contenedores para testing (opcional)

### Distribución por Servicio

| Servicio | Integration Tests | Estado |
|----------|------------------|--------|
| User Service | 8 | ✅ Passing |
| Product Service | 8 | ✅ Passing |
| Order Service | 7 | ✅ Passing |
| Payment Service | 7 | ✅ Passing |
| Shipping Service | 8 | ✅ Passing |
| Favourite Service | 7 | ✅ Passing |
| **Total** | **45** | **✅ 100%** |

### Ejemplo: Order Service Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PaymentServiceProxy paymentServiceProxy;
    
    @Test
    @DisplayName("POST /api/orders - Should create order successfully")
    void testCreateOrder() throws Exception {
        // Given
        OrderRequest request = OrderRequest.builder()
            .userId(1L)
            .productId(100L)
            .quantity(2)
            .build();
            
        // Mock payment service response
        when(paymentServiceProxy.processPayment(any()))
            .thenReturn(new PaymentResponse(true, "Payment successful"));
        
        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andDo(print());
    }
    
    @Test
    @DisplayName("GET /api/orders/{id} - Should return order")
    void testGetOrder() throws Exception {
        // Given
        Long orderId = 1L;
        
        // When & Then
        mockMvc.perform(get("/api/orders/{id}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.userId").exists())
            .andDo(print());
    }
    
    @Test
    @DisplayName("GET /api/orders - Should return paginated orders")
    void testGetAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").exists())
            .andDo(print());
    }
}
```

### Configuración de Testing

**`application-test.properties`:**
```properties
# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# Disable Eureka for testing
eureka.client.enabled=false

# Disable Zipkin for testing
spring.zipkin.enabled=false

# Logging
logging.level.com.selimhorri.app=DEBUG
```

### Comandos de Ejecución

```powershell
# Ejecutar integration tests de un servicio
cd order-service
.\mvnw.cmd verify -DskipUTs=true

# Ejecutar todos los integration tests
.\mvnw.cmd clean verify -DskipUTs=true
```

### Resultados

```
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 🎭 End-to-End Tests

### Descripción

Tests que verifican **flujos completos de usuario** desde el inicio hasta el final, simulando escenarios reales de uso del sistema.

### Estado Actual: ⚠️ NO FUNCIONALES

**Problema:** Los E2E tests no pueden conectarse a los servicios en Minikube debido a problemas de networking y configuración de port-forwarding.

### Tecnologías

- **REST Assured** - Testing de APIs REST
- **JUnit 5** - Framework de testing
- **Awaitility** - Testing asíncrono
- **Spring Boot Test** - Testing utilities

### Tests Implementados

| Test | Pasos | Assertions | Estado |
|------|-------|-----------|--------|
| User Registration Flow | 5 | 18 | ⚠️ No funcional |
| Shopping & Favorites Flow | 6 | 22 | ⚠️ No funcional |
| Order Creation & Processing | 6 | 20 | ⚠️ No funcional |
| Payment Processing Flow | 6 | 18 | ⚠️ No funcional |
| Shipping & Fulfillment Flow | 9 | 28 | ⚠️ No funcional |
| **Total** | **32** | **106** | **⚠️ 0%** |

### Ejemplo: Shopping & Favorites E2E Test

```java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShoppingAndFavoritesFlowE2ETest {

    private static final String API_BASE_URL = 
        System.getProperty("api.base.url", "http://localhost:8080");
    
    private static Long userId;
    private static Long productId;
    private static Long favouriteId;

    @Test
    @Order(1)
    @DisplayName("Step 1: Create test user")
    void createUser() {
        UserRequest request = new UserRequest(
            "shopper_" + System.currentTimeMillis(),
            "shopper@test.com",
            "John",
            "Doe"
        );

        ValidatableResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(API_BASE_URL + "/api/users")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("username", equalTo(request.getUsername()));

        userId = response.extract().path("id");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Browse products")
    void browseProducts() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get(API_BASE_URL + "/api/products")
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("content.size()", greaterThan(0));
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Get product details")
    void getProductDetails() {
        ValidatableResponse response = given()
            .contentType(ContentType.JSON)
        .when()
            .get(API_BASE_URL + "/api/products/1")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", notNullValue())
            .body("price", notNullValue());

        productId = response.extract().path("id");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Add product to favorites")
    void addToFavorites() {
        FavouriteRequest request = new FavouriteRequest(userId, productId);

        ValidatableResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(API_BASE_URL + "/api/favourites")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("userId", equalTo(userId.intValue()))
            .body("productId", equalTo(productId.intValue()));

        favouriteId = response.extract().path("id");
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: View user favorites")
    void viewFavorites() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get(API_BASE_URL + "/api/favourites/user/" + userId)
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(1)))
            .body("[0].userId", equalTo(userId.intValue()))
            .body("[0].productId", equalTo(productId.intValue()));
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Remove from favorites")
    void removeFromFavorites() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .delete(API_BASE_URL + "/api/favourites/" + favouriteId)
        .then()
            .statusCode(204);

        // Verify removal
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            given()
                .contentType(ContentType.JSON)
            .when()
                .get(API_BASE_URL + "/api/favourites/user/" + userId)
            .then()
                .statusCode(200)
                .body("$", hasSize(0));
        });
    }

    @AfterAll
    static void cleanup() {
        // Cleanup test data
        if (userId != null) {
            given().delete(API_BASE_URL + "/api/users/" + userId);
        }
    }
}
```

### Problemas Detectados

#### 🔴 Problema 1: Conectividad con API Gateway

**Error:**
```
java.net.ConnectException: Connection refused: connect
```

**Causa:**
- Port-forward de API Gateway no se mantiene activo
- URL configurada apunta a `http://localhost:8080` pero el servicio no es accesible
- Minikube con Docker driver requiere configuración especial

**Soluciones intentadas:**
1. ✅ Port-forward manual: `kubectl port-forward svc/api-gateway-service 8080:8080`
2. ❌ PowerShell jobs para mantener port-forward activo
3. ❌ Minikube tunnel (requiere permisos admin)
4. ⚠️ Cambio a LoadBalancer (EXTERNAL-IP pending)

#### 🔴 Problema 2: Servicios No Disponibles

**Error:**
```
404 Not Found - /api/users endpoint not available
```

**Causa:**
- Algunos microservicios en CrashLoopBackOff
- Eureka no tiene todos los servicios registrados
- API Gateway no puede enrutar correctamente

#### 🔴 Problema 3: Timeouts en Awaitility

**Error:**
```
org.awaitility.core.ConditionTimeoutException: 
Condition was not fulfilled within 10 seconds
```

**Causa:**
- Operaciones asíncronas tardan más de lo esperado
- Servicios lentos por falta de recursos en Minikube

### Configuración de E2E Tests

**`pom.xml` del módulo `e2e-tests`:**
```xml
<dependencies>
    <!-- REST Assured -->
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <version>5.3.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Awaitility para testing asíncrono -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**`application-test.properties`:**
```properties
# API Gateway URL (configurable)
api.base.url=http://localhost:8080

# Timeouts
rest-assured.timeout=10000

# Retry config
api.retry.max-attempts=3
api.retry.delay=2000
```

### Comandos de Ejecución (Fallidos)

```powershell
# Intentos de ejecución
.\run-e2e-tests.ps1  # ❌ Connection refused
mvn test -pl e2e-tests  # ❌ 404 Not Found
```

---

## 📈 Métricas de Cobertura

### Por Servicio

| Servicio | Line Coverage | Branch Coverage | Estado |
|----------|--------------|----------------|--------|
| User Service | 82% | 71% | ✅ Excelente |
| Product Service | 78% | 68% | ✅ Bueno |
| Order Service | 75% | 65% | ✅ Aceptable |
| Payment Service | 81% | 73% | ✅ Excelente |
| Shipping Service | 77% | 66% | ✅ Bueno |
| Favourite Service | 74% | 63% | ✅ Aceptable |
| **Promedio** | **78%** | **68%** | **✅ Bueno** |

### Gráfico de Cobertura

```
User Service      ████████████████░░  82%
Product Service   ████████████████░░  78%
Order Service     ███████████████░░░  75%
Payment Service   ████████████████░░  81%
Shipping Service  ████████████████░░  77%
Favourite Service ███████████████░░░  74%
```

---

## 🎯 Estrategia de Testing en CI/CD

### Pipeline de Tests

```yaml
# Dev Environment - Unit Tests
stages:
  - test

unit-tests:
  matrix:
    service: [user, product, order, payment, shipping, favourite]
  script:
    - cd ${service}-service
    - ./mvnw.cmd clean test -DskipITs=true
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml
```

```yaml
# Stage Environment - Integration Tests
integration-tests:
  matrix:
    service: [user, product, order, payment, shipping, favourite]
  script:
    - cd ${service}-service
    - ./mvnw.cmd clean verify -DskipUTs=true
  artifacts:
    reports:
      junit: target/failsafe-reports/TEST-*.xml
```

```yaml
# Production - All Tests
all-tests:
  matrix:
    service: [user, product, order, payment, shipping, favourite]
  script:
    - cd ${service}-service
    - ./mvnw.cmd clean verify
```

---

## 🔮 Próximos Pasos para E2E Tests

### Soluciones Propuestas

1. **Usar Minikube Tunnel (Recomendado)**
   ```powershell
   # Ejecutar como Administrador
   minikube tunnel
   ```

2. **Configurar NodePort Services**
   ```yaml
   spec:
     type: NodePort
     ports:
     - port: 8080
       nodePort: 30080
   ```
   Acceso: `http://<minikube-ip>:30080`

3. **Desplegar en Cluster Real**
   - Usar AKS, EKS, o GKE
   - Configurar Ingress Controllers
   - Load Balancers reales

4. **Mock de Servicios Externos**
   - Usar WireMock para simular APIs
   - Evitar dependencia de Minikube

---

## 📚 Referencias

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [REST Assured](https://rest-assured.io/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Awaitility](http://www.awaitility.org/)

---

**Próximo paso:** [CI/CD Pipelines](./04-CICD-PIPELINES.md)
