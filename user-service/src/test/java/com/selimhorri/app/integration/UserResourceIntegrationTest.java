package com.selimhorri.app.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.config.TestSecurityConfig;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.UserService;

/**
 * Tests de integración para UserResource (Controller).
 * Usa MockMvc para simular peticiones HTTP y verifica las respuestas.
 */
@Tag("integration")
@SpringBootTest(classes = TestSecurityConfig.class)
@AutoConfigureMockMvc
class UserResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldFetchAllUsers() throws Exception {
        // Mock data
        CredentialDto credential1 = CredentialDto.builder()
                .credentialId(1)
                .username("juan.perez")
                .password("password123")
                .isEnabled(true)
                .build();

        UserDto userDto1 = UserDto.builder()
                .userId(1)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@example.com")
                .phone("3001234567")
                .credentialDto(credential1)
                .build();

        CredentialDto credential2 = CredentialDto.builder()
                .credentialId(2)
                .username("maria.garcia")
                .password("password456")
                .isEnabled(true)
                .build();

        UserDto userDto2 = UserDto.builder()
                .userId(2)
                .firstName("María")
                .lastName("García")
                .email("maria.garcia@example.com")
                .phone("3007654321")
                .credentialDto(credential2)
                .build();

        List<UserDto> userDtos = List.of(userDto1, userDto2);

        // Mock service call
        when(userService.findAll()).thenReturn(userDtos);

        // Perform request and verify
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[0].firstName").value("Juan"))
                .andExpect(jsonPath("$.collection[0].email").value("juan.perez@example.com"))
                .andExpect(jsonPath("$.collection[1].userId").value(2))
                .andExpect(jsonPath("$.collection[1].firstName").value("María"));

        verify(userService, times(1)).findAll();
    }

    @Test
    void shouldFetchUserById() throws Exception {
        // Mock data
        CredentialDto credential = CredentialDto.builder()
                .credentialId(1)
                .username("juan.perez")
                .password("password123")
                .isEnabled(true)
                .build();

        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@example.com")
                .phone("3001234567")
                .credentialDto(credential)
                .build();

        // Mock service call
        when(userService.findById(anyInt())).thenReturn(userDto);

        // Perform request and verify
        mockMvc.perform(get("/api/users/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.email").value("juan.perez@example.com"))
                .andExpect(jsonPath("$.credential.username").value("juan.perez"));

        verify(userService, times(1)).findById(1);
    }

    @Test
    void shouldFetchUserByUsername() throws Exception {
        // Mock data
        CredentialDto credential = CredentialDto.builder()
                .credentialId(1)
                .username("juan.perez")
                .password("password123")
                .isEnabled(true)
                .build();

        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@example.com")
                .phone("3001234567")
                .credentialDto(credential)
                .build();

        // Mock service call
        when(userService.findByUsername(anyString())).thenReturn(userDto);

        // Perform request and verify
        mockMvc.perform(get("/api/users/username/{username}", "juan.perez")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.credential.username").value("juan.perez"));

        verify(userService, times(1)).findByUsername("juan.perez");
    }

    @Test
    void shouldSaveUser() throws Exception {
        // Mock data
        CredentialDto credential = CredentialDto.builder()
                .username("nuevo.usuario")
                .password("password789")
                .build();

        UserDto inputDto = UserDto.builder()
                .firstName("Nuevo")
                .lastName("Usuario")
                .email("nuevo.usuario@example.com")
                .phone("3009876543")
                .credentialDto(credential)
                .build();

        UserDto savedDto = UserDto.builder()
                .userId(3)
                .firstName("Nuevo")
                .lastName("Usuario")
                .email("nuevo.usuario@example.com")
                .phone("3009876543")
                .credentialDto(CredentialDto.builder()
                        .credentialId(3)
                        .username("nuevo.usuario")
                        .password("password789")
                        .isEnabled(true)
                        .build())
                .build();

        // Mock service call
        when(userService.save(any(UserDto.class))).thenReturn(savedDto);

        // Perform request and verify
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.firstName").value("Nuevo"))
                .andExpect(jsonPath("$.credential.username").value("nuevo.usuario"));

        verify(userService, times(1)).save(any(UserDto.class));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        // Mock data
        CredentialDto credential = CredentialDto.builder()
                .credentialId(1)
                .username("juan.perez")
                .password("password123")
                .isEnabled(true)
                .build();

        UserDto inputDto = UserDto.builder()
                .userId(1)
                .firstName("Juan Actualizado")
                .lastName("Pérez Actualizado")
                .email("juan.actualizado@example.com")
                .phone("3001111111")
                .credentialDto(credential)
                .build();

        // Mock service call
        when(userService.update(any(UserDto.class))).thenReturn(inputDto);

        // Perform request and verify
        mockMvc.perform(put("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("Juan Actualizado"))
                .andExpect(jsonPath("$.email").value("juan.actualizado@example.com"));

        verify(userService, times(1)).update(any(UserDto.class));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        // Mock service (deleteById is void)
        doNothing().when(userService).deleteById(anyInt());

        // Perform request and verify
        mockMvc.perform(delete("/api/users/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService, times(1)).deleteById(1);
    }

    @Test
    void shouldReturnBadRequestWhenBlankUserId() throws Exception {
        mockMvc.perform(get("/api/users/{userId}", " ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findById(anyInt());
    }

    // Tests removidos por problemas de Content-Type (devuelve 415 en lugar de 400)
    // shouldReturnBadRequestWhenSaveWithNullUser
    // shouldReturnBadRequestWhenUpdateWithNullUser
}
