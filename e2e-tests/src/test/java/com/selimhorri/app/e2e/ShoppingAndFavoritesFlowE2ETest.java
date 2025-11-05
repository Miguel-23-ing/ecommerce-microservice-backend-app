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

import java.util.concurrent.TimeUnit;

/**
 * E2E Test 2: Complete Shopping Flow - Browse Products and Add to Favorites
 * 
 * Este test valida el flujo completo de:
 * 1. Crear un usuario
 * 2. Buscar y listar productos disponibles
 * 3. Consultar detalles de un producto específico
 * 4. Agregar producto a favoritos
 * 5. Verificar que el favorito existe
 * 6. Eliminar el favorito
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class ShoppingAndFavoritesFlowE2ETest {

    private static final String API_BASE_URL = System.getProperty("api.gateway.url", "http://localhost:8080");
    private static final String USER_SERVICE_URL = API_BASE_URL + "/api/users";
    private static final String PRODUCT_SERVICE_URL = API_BASE_URL + "/api/products";
    private static final String FAVOURITE_SERVICE_URL = API_BASE_URL + "/api/favourites";
    
    private static Integer testUserId;
    private static Integer testProductId;
    private static Integer createdFavouriteId;
    private static String testUsername;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        testUsername = "e2e_shopper_" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-2.1: Crear usuario comprador")
    void shouldCreateShopperUser() {
        String newUserJson = String.format("""
            {
                "firstName": "Shopper",
                "lastName": "E2ETest",
                "email": "%s@test.com",
                "phone": "+1234567890",
                "credential": {
                    "username": "%s",
                    "password": "Pass123!",
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
            .body("firstName", equalTo("Shopper"))
        .extract()
            .response();

        testUserId = response.jsonPath().getInt("userId");
        assertNotNull(testUserId);
        System.out.println("✅ Usuario comprador creado con ID: " + testUserId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E-2.2: Listar productos disponibles")
    void shouldBrowseAvailableProducts() {
        Response response = given()
        .when()
            .get(PRODUCT_SERVICE_URL)
        .then()
            .statusCode(200)
            .body("$", not(empty()))
        .extract()
            .response();

        // Obtener el primer producto disponible
        testProductId = response.jsonPath().getInt("[0].productId");
        assertNotNull(testProductId);
        System.out.println("✅ Productos listados. Seleccionado producto ID: " + testProductId);
    }

    @Test
    @Order(3)
    @DisplayName("E2E-2.3: Consultar detalles del producto seleccionado")
    void shouldViewProductDetails() {
        assertNotNull(testProductId);

        given()
            .pathParam("productId", testProductId)
        .when()
            .get(PRODUCT_SERVICE_URL + "/{productId}")
        .then()
            .statusCode(200)
            .body("productId", equalTo(testProductId))
            .body("productTitle", notNullValue())
            .body("sku", notNullValue());

        System.out.println("✅ Detalles del producto consultados correctamente");
    }

    @Test
    @Order(4)
    @DisplayName("E2E-2.4: Agregar producto a favoritos")
    void shouldAddProductToFavourites() {
        assertNotNull(testUserId);
        assertNotNull(testProductId);

        String favouriteJson = String.format("""
            {
                "user": {
                    "userId": %d
                },
                "product": {
                    "productId": %d
                },
                "likeDate": "%s"
            }
            """, testUserId, testProductId, java.time.LocalDateTime.now());

        Response response = given()
            .contentType(ContentType.JSON)
            .body(favouriteJson)
        .when()
            .post(FAVOURITE_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("user.userId", equalTo(testUserId))
            .body("product.productId", equalTo(testProductId))
        .extract()
            .response();

        createdFavouriteId = response.jsonPath().getInt("favouriteId");
        assertNotNull(createdFavouriteId);
        System.out.println("✅ Producto agregado a favoritos con ID: " + createdFavouriteId);
    }

    @Test
    @Order(5)
    @DisplayName("E2E-2.5: Verificar que el favorito existe")
    void shouldVerifyFavouriteExists() {
        assertNotNull(createdFavouriteId);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .pathParam("favouriteId", createdFavouriteId)
                .when()
                    .get(FAVOURITE_SERVICE_URL + "/{favouriteId}")
                .then()
                    .statusCode(200)
                    .body("favouriteId", equalTo(createdFavouriteId))
                    .body("user.userId", equalTo(testUserId))
                    .body("product.productId", equalTo(testProductId));
            });

        System.out.println("✅ Favorito verificado exitosamente");
    }

    @Test
    @Order(6)
    @DisplayName("E2E-2.6: Listar todos los favoritos del usuario")
    void shouldListUserFavourites() {
        given()
        .when()
            .get(FAVOURITE_SERVICE_URL)
        .then()
            .statusCode(200)
            .body("$", not(empty()))
            .body("find { it.favouriteId == " + createdFavouriteId + " }.user.userId", equalTo(testUserId));

        System.out.println("✅ Favoritos del usuario listados correctamente");
    }

    @AfterAll
    static void cleanup() {
        // Cleanup: eliminar favorito y usuario
        if (createdFavouriteId != null) {
            try {
                given()
                    .pathParam("favouriteId", createdFavouriteId)
                .when()
                    .delete(FAVOURITE_SERVICE_URL + "/{favouriteId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                System.out.println("🧹 Favorito eliminado (ID: " + createdFavouriteId + ")");
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
