package com.selimhorri.app.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * E2E Test 3: Complete Order Creation and Processing Flow
 * 
 * Este test valida el flujo completo de:
 * 1. Crear usuario
 * 2. Seleccionar producto
 * 3. Crear una orden de compra
 * 4. Verificar que la orden fue creada correctamente
 * 5. Actualizar el estado de la orden
 * 6. Consultar historial de órdenes del usuario
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class OrderCreationAndProcessingFlowE2ETest {

    private static final String API_BASE_URL = System.getProperty("api.gateway.url", "http://localhost:8080");
    private static final String USER_SERVICE_URL = API_BASE_URL + "/api/users";
    private static final String PRODUCT_SERVICE_URL = API_BASE_URL + "/api/products";
    private static final String ORDER_SERVICE_URL = API_BASE_URL + "/api/orders";
    
    private static Integer testUserId;
    private static Integer testProductId;
    private static Integer createdOrderId;
    private static String testUsername;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        testUsername = "e2e_buyer_" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-3.1: Crear usuario comprador")
    void shouldCreateBuyerUser() {
        String newUserJson = String.format("""
            {
                "firstName": "Buyer",
                "lastName": "E2ETest",
                "email": "%s@test.com",
                "phone": "+1234567890",
                "credential": {
                    "username": "%s",
                    "password": "BuyerPass123!",
                    "isEnabled": true,
                    "isAccountNonExpired": true,
                    "isAccountNonLocked": true,
                    "isCredentialsNonExpired": true
                }
            }
            """, testUsername, testUsername);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(newUserJson)
        .when()
            .post(USER_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(201)))
        .extract()
            .response();

        testUserId = response.jsonPath().getInt("userId");
        assertNotNull(testUserId);
        System.out.println("✅ Usuario comprador creado con ID: " + testUserId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E-3.2: Seleccionar producto para comprar")
    void shouldSelectProductToBuy() {
        Response response = given()
        .when()
            .get(PRODUCT_SERVICE_URL)
        .then()
            .statusCode(200)
            .body("$", not(empty()))
        .extract()
            .response();

        testProductId = response.jsonPath().getInt("[0].productId");
        assertNotNull(testProductId);
        System.out.println("✅ Producto seleccionado ID: " + testProductId);
    }

    @Test
    @Order(3)
    @DisplayName("E2E-3.3: Crear orden de compra")
    void shouldCreateOrder() {
        assertNotNull(testUserId);
        assertNotNull(testProductId);

        String orderDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String orderJson = String.format("""
            {
                "orderDate": "%s",
                "orderDesc": "E2E Test Order",
                "user": {
                    "userId": %d
                },
                "cart": {
                    "cartId": 1
                }
            }
            """, orderDate, testUserId);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(orderJson)
        .when()
            .post(ORDER_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("orderDesc", equalTo("E2E Test Order"))
            .body("user.userId", equalTo(testUserId))
        .extract()
            .response();

        createdOrderId = response.jsonPath().getInt("orderId");
        assertNotNull(createdOrderId);
        System.out.println("✅ Orden creada con ID: " + createdOrderId);
    }

    @Test
    @Order(4)
    @DisplayName("E2E-3.4: Verificar que la orden fue creada correctamente")
    void shouldVerifyOrderCreation() {
        assertNotNull(createdOrderId);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .pathParam("orderId", createdOrderId)
                .when()
                    .get(ORDER_SERVICE_URL + "/{orderId}")
                .then()
                    .statusCode(200)
                    .body("orderId", equalTo(createdOrderId))
                    .body("orderDesc", equalTo("E2E Test Order"))
                    .body("user.userId", equalTo(testUserId));
            });

        System.out.println("✅ Orden verificada exitosamente");
    }

    @Test
    @Order(5)
    @DisplayName("E2E-3.5: Actualizar estado de la orden")
    void shouldUpdateOrderStatus() {
        assertNotNull(createdOrderId);
        assertNotNull(testUserId);

        String orderDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String updatedOrderJson = String.format("""
            {
                "orderId": %d,
                "orderDate": "%s",
                "orderDesc": "E2E Test Order - Processing",
                "user": {
                    "userId": %d
                },
                "cart": {
                    "cartId": 1
                }
            }
            """, createdOrderId, orderDate, testUserId);

        given()
            .contentType(ContentType.JSON)
            .body(updatedOrderJson)
        .when()
            .put(ORDER_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(202)))
            .body("orderId", equalTo(createdOrderId))
            .body("orderDesc", containsString("Processing"));

        System.out.println("✅ Estado de orden actualizado");
    }

    @Test
    @Order(6)
    @DisplayName("E2E-3.6: Consultar historial de órdenes")
    void shouldViewOrderHistory() {
        given()
        .when()
            .get(ORDER_SERVICE_URL)
        .then()
            .statusCode(200)
            .body("$", not(empty()))
            .body("find { it.orderId == " + createdOrderId + " }.orderDesc", containsString("Processing"));

        System.out.println("✅ Historial de órdenes consultado exitosamente");
    }

    @AfterAll
    static void cleanup() {
        if (createdOrderId != null) {
            try {
                given()
                    .pathParam("orderId", createdOrderId)
                .when()
                    .delete(ORDER_SERVICE_URL + "/{orderId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                System.out.println("🧹 Orden eliminada (ID: " + createdOrderId + ")");
            } catch (Exception e) {
                System.err.println("⚠️  Cleanup warning: " + e.getMessage());
            }
        }

        if (testUserId != null) {
            try {
                given()
                    .pathParam("userId", testUserId)
                .when()
                    .delete(USER_SERVICE_URL + "/{userId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                System.out.println("🧹 Usuario eliminado (ID: " + testUserId + ")");
            } catch (Exception e) {
                System.err.println("⚠️  Cleanup warning: " + e.getMessage());
            }
        }
    }
}
