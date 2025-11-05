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
 * E2E Test 4: Complete Payment Processing Flow
 * 
 * Este test valida el flujo completo de:
 * 1. Crear usuario
 * 2. Crear orden de compra
 * 3. Procesar pago de la orden
 * 4. Verificar que el pago fue registrado
 * 5. Consultar detalles del pago
 * 6. Listar todos los pagos
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class PaymentProcessingFlowE2ETest {

    private static final String API_BASE_URL = System.getProperty("api.gateway.url", "http://localhost:8080");
    private static final String USER_SERVICE_URL = API_BASE_URL + "/api/users";
    private static final String ORDER_SERVICE_URL = API_BASE_URL + "/api/orders";
    private static final String PAYMENT_SERVICE_URL = API_BASE_URL + "/api/payments";
    
    private static Integer testUserId;
    private static Integer testOrderId;
    private static Integer createdPaymentId;
    private static String testUsername;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        testUsername = "e2e_payer_" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-4.1: Crear usuario para procesar pago")
    void shouldCreatePayerUser() {
        String newUserJson = String.format("""
            {
                "firstName": "Payer",
                "lastName": "E2ETest",
                "email": "%s@test.com",
                "phone": "+1234567890",
                "credential": {
                    "username": "%s",
                    "password": "PayerPass123!",
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
        System.out.println("✅ Usuario pagador creado con ID: " + testUserId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E-4.2: Crear orden para procesar pago")
    void shouldCreateOrderForPayment() {
        assertNotNull(testUserId);

        String orderDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String orderJson = String.format("""
            {
                "orderDate": "%s",
                "orderDesc": "E2E Payment Test Order",
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
        System.out.println("✅ Orden creada para pago con ID: " + testOrderId);
    }

    @Test
    @Order(3)
    @DisplayName("E2E-4.3: Procesar pago de la orden")
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
            .body("order.orderId", equalTo(testOrderId))
        .extract()
            .response();

        createdPaymentId = response.jsonPath().getInt("paymentId");
        assertNotNull(createdPaymentId);
        System.out.println("✅ Pago procesado con ID: " + createdPaymentId);
    }

    @Test
    @Order(4)
    @DisplayName("E2E-4.4: Verificar que el pago fue registrado")
    void shouldVerifyPaymentRegistration() {
        assertNotNull(createdPaymentId);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .pathParam("paymentId", createdPaymentId)
                .when()
                    .get(PAYMENT_SERVICE_URL + "/{paymentId}")
                .then()
                    .statusCode(200)
                    .body("paymentId", equalTo(createdPaymentId))
                    .body("isPayed", equalTo(true))
                    .body("order.orderId", equalTo(testOrderId));
            });

        System.out.println("✅ Pago verificado exitosamente");
    }

    @Test
    @Order(5)
    @DisplayName("E2E-4.5: Actualizar estado del pago")
    void shouldUpdatePaymentStatus() {
        assertNotNull(createdPaymentId);
        assertNotNull(testOrderId);

        String paymentDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        String updatedPaymentJson = String.format("""
            {
                "paymentId": %d,
                "isPayed": true,
                "paymentDate": "%s",
                "order": {
                    "orderId": %d
                }
            }
            """, createdPaymentId, paymentDate, testOrderId);

        given()
            .contentType(ContentType.JSON)
            .body(updatedPaymentJson)
        .when()
            .put(PAYMENT_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(202)))
            .body("paymentId", equalTo(createdPaymentId))
            .body("isPayed", equalTo(true));

        System.out.println("✅ Estado de pago actualizado");
    }

    @Test
    @Order(6)
    @DisplayName("E2E-4.6: Listar todos los pagos incluyendo el procesado")
    void shouldListAllPayments() {
        given()
        .when()
            .get(PAYMENT_SERVICE_URL)
        .then()
            .statusCode(200)
            .body("$", not(empty()))
            .body("find { it.paymentId == " + createdPaymentId + " }.isPayed", equalTo(true));

        System.out.println("✅ Pagos listados exitosamente");
    }

    @AfterAll
    static void cleanup() {
        if (createdPaymentId != null) {
            try {
                given()
                    .pathParam("paymentId", createdPaymentId)
                .when()
                    .delete(PAYMENT_SERVICE_URL + "/{paymentId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                System.out.println("🧹 Pago eliminado (ID: " + createdPaymentId + ")");
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
