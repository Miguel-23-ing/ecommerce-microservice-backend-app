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
 * E2E Test 5: Complete Shipping and Order Fulfillment Flow
 * 
 * Este test valida el flujo completo de:
 * 1. Crear usuario
 * 2. Crear orden de compra
 * 3. Procesar pago
 * 4. Crear registro de envío (shipping)
 * 5. Actualizar estado del envío
 * 6. Verificar el tracking completo del pedido
 * 7. Consultar historial de envíos
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class ShippingAndFulfillmentFlowE2ETest {

    private static final String API_BASE_URL = System.getProperty("api.gateway.url", "http://localhost:8080");
    private static final String USER_SERVICE_URL = API_BASE_URL + "/api/users";
    private static final String PRODUCT_SERVICE_URL = API_BASE_URL + "/api/products";
    private static final String ORDER_SERVICE_URL = API_BASE_URL + "/api/orders";
    private static final String PAYMENT_SERVICE_URL = API_BASE_URL + "/api/payments";
    private static final String SHIPPING_SERVICE_URL = API_BASE_URL + "/api/order-items";
    
    private static Integer testUserId;
    private static Integer testProductId;
    private static Integer testOrderId;
    private static Integer testPaymentId;
    private static Integer createdOrderItemId;
    private static String testUsername;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        testUsername = "e2e_shipper_" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-5.1: Crear usuario para flujo completo")
    void shouldCreateCustomerUser() {
        String newUserJson = String.format("""
            {
                "firstName": "Customer",
                "lastName": "E2EFullFlow",
                "email": "%s@test.com",
                "phone": "+1234567890",
                "credential": {
                    "username": "%s",
                    "password": "CustomerPass123!",
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
        System.out.println("✅ Usuario cliente creado con ID: " + testUserId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E-5.2: Seleccionar producto para envío")
    void shouldSelectProduct() {
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
    @DisplayName("E2E-5.3: Crear orden de compra")
    void shouldCreateOrder() {
        assertNotNull(testUserId);

        String orderDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String orderJson = String.format("""
            {
                "orderDate": "%s",
                "orderDesc": "E2E Shipping Test Order",
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
        .extract()
            .response();

        testOrderId = response.jsonPath().getInt("orderId");
        assertNotNull(testOrderId);
        System.out.println("✅ Orden creada con ID: " + testOrderId);
    }

    @Test
    @Order(4)
    @DisplayName("E2E-5.4: Procesar pago de la orden")
    void shouldProcessPayment() {
        assertNotNull(testOrderId);

        String paymentDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String paymentJson = String.format("""
            {
                "isPayed": true,
                "paymentDate": "%s",
                "order": {
                    "orderId": %d
                }
            }
            """, paymentDate, testOrderId);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(paymentJson)
        .when()
            .post(PAYMENT_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("isPayed", equalTo(true))
        .extract()
            .response();

        testPaymentId = response.jsonPath().getInt("paymentId");
        assertNotNull(testPaymentId);
        System.out.println("✅ Pago procesado con ID: " + testPaymentId);
    }

    @Test
    @Order(5)
    @DisplayName("E2E-5.5: Crear registro de envío (order item)")
    void shouldCreateShippingRecord() {
        assertNotNull(testOrderId);
        assertNotNull(testProductId);

        String orderItemJson = String.format("""
            {
                "orderedQuantity": 2,
                "product": {
                    "productId": %d
                },
                "order": {
                    "orderId": %d
                }
            }
            """, testProductId, testOrderId);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(orderItemJson)
        .when()
            .post(SHIPPING_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("orderedQuantity", equalTo(2))
            .body("product.productId", equalTo(testProductId))
            .body("order.orderId", equalTo(testOrderId))
        .extract()
            .response();

        createdOrderItemId = response.jsonPath().getInt("orderItemId");
        assertNotNull(createdOrderItemId);
        System.out.println("✅ Registro de envío creado con ID: " + createdOrderItemId);
    }

    @Test
    @Order(6)
    @DisplayName("E2E-5.6: Verificar registro de envío")
    void shouldVerifyShippingRecord() {
        assertNotNull(createdOrderItemId);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .pathParam("orderItemId", createdOrderItemId)
                .when()
                    .get(SHIPPING_SERVICE_URL + "/{orderItemId}")
                .then()
                    .statusCode(200)
                    .body("orderItemId", equalTo(createdOrderItemId))
                    .body("orderedQuantity", equalTo(2))
                    .body("order.orderId", equalTo(testOrderId));
            });

        System.out.println("✅ Registro de envío verificado");
    }

    @Test
    @Order(7)
    @DisplayName("E2E-5.7: Actualizar cantidad en el envío")
    void shouldUpdateShippingQuantity() {
        assertNotNull(createdOrderItemId);
        assertNotNull(testOrderId);
        assertNotNull(testProductId);

        String updatedOrderItemJson = String.format("""
            {
                "orderItemId": %d,
                "orderedQuantity": 3,
                "product": {
                    "productId": %d
                },
                "order": {
                    "orderId": %d
                }
            }
            """, createdOrderItemId, testProductId, testOrderId);

        given()
            .contentType(ContentType.JSON)
            .body(updatedOrderItemJson)
        .when()
            .put(SHIPPING_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(202)))
            .body("orderItemId", equalTo(createdOrderItemId))
            .body("orderedQuantity", equalTo(3));

        System.out.println("✅ Cantidad de envío actualizada");
    }

    @Test
    @Order(8)
    @DisplayName("E2E-5.8: Consultar historial completo de envíos")
    void shouldViewShippingHistory() {
        given()
        .when()
            .get(SHIPPING_SERVICE_URL)
        .then()
            .statusCode(200)
            .body("$", not(empty()))
            .body("find { it.orderItemId == " + createdOrderItemId + " }.orderedQuantity", equalTo(3));

        System.out.println("✅ Historial de envíos consultado exitosamente");
    }

    @Test
    @Order(9)
    @DisplayName("E2E-5.9: Verificar tracking completo del pedido")
    void shouldVerifyCompleteOrderTracking() {
        // Verificar que la orden existe
        given()
            .pathParam("orderId", testOrderId)
        .when()
            .get(ORDER_SERVICE_URL + "/{orderId}")
        .then()
            .statusCode(200)
            .body("orderId", equalTo(testOrderId));

        // Verificar que el pago existe
        given()
            .pathParam("paymentId", testPaymentId)
        .when()
            .get(PAYMENT_SERVICE_URL + "/{paymentId}")
        .then()
            .statusCode(200)
            .body("isPayed", equalTo(true));

        // Verificar que el envío existe
        given()
            .pathParam("orderItemId", createdOrderItemId)
        .when()
            .get(SHIPPING_SERVICE_URL + "/{orderItemId}")
        .then()
            .statusCode(200)
            .body("orderItemId", equalTo(createdOrderItemId));

        System.out.println("✅ Tracking completo del pedido verificado - Orden → Pago → Envío");
    }

    @AfterAll
    static void cleanup() {
        if (createdOrderItemId != null) {
            try {
                given()
                    .pathParam("orderItemId", createdOrderItemId)
                .when()
                    .delete(SHIPPING_SERVICE_URL + "/{orderItemId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                System.out.println("🧹 OrderItem eliminado (ID: " + createdOrderItemId + ")");
            } catch (Exception e) {
                System.err.println("⚠️  Cleanup warning: " + e.getMessage());
            }
        }

        if (testPaymentId != null) {
            try {
                given()
                    .pathParam("paymentId", testPaymentId)
                .when()
                    .delete(PAYMENT_SERVICE_URL + "/{paymentId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                System.out.println("🧹 Pago eliminado (ID: " + testPaymentId + ")");
            } catch (Exception e) {
                System.err.println("⚠️  Cleanup warning: " + e.getMessage());
            }
        }

        if (testOrderId != null) {
            try {
                given()
                    .pathParam("orderId", testOrderId)
                .when()
                    .delete(ORDER_SERVICE_URL + "/{orderId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                System.out.println("🧹 Orden eliminada (ID: " + testOrderId + ")");
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
