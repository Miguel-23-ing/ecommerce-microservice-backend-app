package com.selimhorri.app.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

/**
 * E2E Test 1: Complete User Registration and Authentication Flow
 * 
 * Este test valida el flujo completo de:
 * 1. Registro de un nuevo usuario
 * 2. Verificación de que el usuario existe
 * 3. Actualización de información del usuario
 * 4. Consulta del perfil actualizado
 * 5. Eliminación del usuario (cleanup)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class UserRegistrationFlowE2ETest {

    private static final String API_BASE_URL = System.getProperty("api.gateway.url", "http://localhost:8080");
    private static final String USER_SERVICE_URL = API_BASE_URL + "/api/users";
    
    private static Integer createdUserId;
    private static String testUsername;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = API_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        testUsername = "e2e_user_" + System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-1.1: Crear nuevo usuario con credenciales")
    void shouldCreateNewUserWithCredentials() {
        String newUserJson = String.format("""
            {
                "firstName": "E2E",
                "lastName": "TestUser",
                "imageUrl": "https://avatar.test/e2e.jpg",
                "email": "%s@test.com",
                "phone": "+1234567890",
                "credential": {
                    "username": "%s",
                    "password": "SecurePass123!",
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
            .body("firstName", equalTo("E2E"))
            .body("lastName", equalTo("TestUser"))
            .body("email", equalTo(testUsername + "@test.com"))
            .body("credential.username", equalTo(testUsername))
        .extract()
            .response();

        createdUserId = response.jsonPath().getInt("userId");
        assertNotNull(createdUserId, "User ID should not be null");
        
        System.out.println("✅ Usuario creado exitosamente con ID: " + createdUserId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E-1.2: Verificar que el usuario existe y puede ser consultado")
    void shouldFetchCreatedUser() {
        assertNotNull(createdUserId, "User must be created first");

        // Esperar a que el usuario esté disponible (eventual consistency)
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                given()
                    .pathParam("userId", createdUserId)
                .when()
                    .get(USER_SERVICE_URL + "/{userId}")
                .then()
                    .statusCode(200)
                    .body("userId", equalTo(createdUserId))
                    .body("firstName", equalTo("E2E"))
                    .body("credential.username", equalTo(testUsername));
            });

        System.out.println("✅ Usuario consultado exitosamente");
    }

    @Test
    @Order(3)
    @DisplayName("E2E-1.3: Actualizar información del usuario")
    void shouldUpdateUserInformation() {
        assertNotNull(createdUserId, "User must be created first");

        String updatedUserJson = String.format("""
            {
                "userId": %d,
                "firstName": "E2E Updated",
                "lastName": "TestUser Modified",
                "imageUrl": "https://avatar.test/e2e-updated.jpg",
                "email": "%s@test.com",
                "phone": "+1234567890",
                "credential": {
                    "username": "%s",
                    "password": "SecurePass123!",
                    "isEnabled": true,
                    "isAccountNonExpired": true,
                    "isAccountNonLocked": true,
                    "isCredentialsNonExpired": true
                }
            }
            """, createdUserId, testUsername, testUsername);

        given()
            .contentType(ContentType.JSON)
            .body(updatedUserJson)
        .when()
            .put(USER_SERVICE_URL)
        .then()
            .statusCode(anyOf(is(200), is(202)))
            .body("firstName", equalTo("E2E Updated"))
            .body("lastName", equalTo("TestUser Modified"));

        System.out.println("✅ Usuario actualizado exitosamente");
    }

    @Test
    @Order(4)
    @DisplayName("E2E-1.4: Verificar actualización del perfil")
    void shouldFetchUpdatedUserProfile() {
        assertNotNull(createdUserId, "User must be created first");

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                given()
                    .pathParam("userId", createdUserId)
                .when()
                    .get(USER_SERVICE_URL + "/{userId}")
                .then()
                    .statusCode(200)
                    .body("userId", equalTo(createdUserId))
                    .body("firstName", equalTo("E2E Updated"))
                    .body("lastName", equalTo("TestUser Modified"));
            });

        System.out.println("✅ Perfil actualizado verificado");
    }

    @Test
    @Order(5)
    @DisplayName("E2E-1.5: Listar todos los usuarios incluyendo el creado")
    void shouldListAllUsersIncludingCreated() {
        given()
        .when()
            .get(USER_SERVICE_URL)
        .then()
            .statusCode(200)
            .body("$", not(empty()))
            .body("find { it.userId == " + createdUserId + " }.firstName", equalTo("E2E Updated"));

        System.out.println("✅ Usuario encontrado en la lista de todos los usuarios");
    }

    @AfterAll
    static void cleanup() {
        if (createdUserId != null) {
            try {
                given()
                    .pathParam("userId", createdUserId)
                .when()
                    .delete(USER_SERVICE_URL + "/{userId}")
                .then()
                    .statusCode(anyOf(is(200), is(204), is(404)));
                
                System.out.println("🧹 Cleanup: Usuario eliminado (ID: " + createdUserId + ")");
            } catch (Exception e) {
                System.err.println("⚠️  Cleanup warning: " + e.getMessage());
            }
        }
    }
}
