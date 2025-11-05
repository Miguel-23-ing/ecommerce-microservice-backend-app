package com.selimhorri.app.integration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.FavouriteService;

/**
 * Tests de integración para FavouriteResource (Controller).
 * Usa MockMvc para simular peticiones HTTP y verifica las respuestas.
 * Nota: FavouriteService usa FavouriteId (clave compuesta: userId + productId + likeDate).
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class FavouriteResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavouriteService favouriteService;

    private ObjectMapper objectMapper;
    private LocalDateTime testLikeDate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testLikeDate = LocalDateTime.of(2025, 11, 4, 10, 30, 0);
    }

    @Test
    void shouldFetchAllFavourites() throws Exception {
        // Mock data
        UserDto user = UserDto.builder()
                .userId(1)
                .firstName("Juan")
                .lastName("Pérez")
                .build();

        ProductDto product1 = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop HP")
                .priceUnit(2500.00)
                .build();

        FavouriteDto favouriteDto1 = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .userDto(user)
                .productDto(product1)
                .likeDate(testLikeDate)
                .build();

        ProductDto product2 = ProductDto.builder()
                .productId(2)
                .productTitle("Mouse Logitech")
                .priceUnit(150.00)
                .build();

        FavouriteDto favouriteDto2 = FavouriteDto.builder()
                .userId(1)
                .productId(2)
                .userDto(user)
                .productDto(product2)
                .likeDate(testLikeDate)
                .build();

        List<FavouriteDto> favouriteDtos = List.of(favouriteDto1, favouriteDto2);

        // Mock service call
        when(favouriteService.findAll()).thenReturn(favouriteDtos);

        // Perform request and verify
        mockMvc.perform(get("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].product.productTitle").value("Laptop HP"))
                .andExpect(jsonPath("$.collection[1].userId").value(1))
                .andExpect(jsonPath("$.collection[1].productId").value(2))
                .andExpect(jsonPath("$.collection[1].product.productTitle").value("Mouse Logitech"));

        verify(favouriteService, times(1)).findAll();
    }

    @Test
    void shouldFetchFavouriteById() throws Exception {
        // Mock data
        UserDto user = UserDto.builder()
                .userId(1)
                .firstName("Juan")
                .lastName("Pérez")
                .build();

        ProductDto product = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop HP")
                .priceUnit(2500.00)
                .build();

        FavouriteDto favouriteDto = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .userDto(user)
                .productDto(product)
                .likeDate(testLikeDate)
                .build();

        // Mock service call - usa FavouriteId compuesto
        when(favouriteService.findById(any(FavouriteId.class))).thenReturn(favouriteDto);

        // Perform request and verify (endpoint: /api/favourites/{userId}/{productId})
        mockMvc.perform(get("/api/favourites/{userId}/{productId}", 1, 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.product.productTitle").value("Laptop HP"));

        verify(favouriteService, times(1)).findById(any(FavouriteId.class));
    }

    @Test
    void shouldSaveFavourite() throws Exception {
        // Mock data
        UserDto user = UserDto.builder()
                .userId(1)
                .build();

        ProductDto product = ProductDto.builder()
                .productId(3)
                .build();

        FavouriteDto inputDto = FavouriteDto.builder()
                .userId(1)
                .productId(3)
                .userDto(user)
                .productDto(product)
                .build();

        FavouriteDto savedDto = FavouriteDto.builder()
                .userId(1)
                .productId(3)
                .userDto(user)
                .productDto(product)
                .likeDate(testLikeDate)
                .build();

        // Mock service call
        when(favouriteService.save(any(FavouriteDto.class))).thenReturn(savedDto);

        // Perform request and verify
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(3));

        verify(favouriteService, times(1)).save(any(FavouriteDto.class));
    }

    @Test
    void shouldDeleteFavourite() throws Exception {
        // Mock service (deleteById is void)
        doNothing().when(favouriteService).deleteById(any(FavouriteId.class));

        // Perform request and verify (endpoint: /api/favourites/{userId}/{productId})
        mockMvc.perform(delete("/api/favourites/{userId}/{productId}", 1, 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(favouriteService, times(1)).deleteById(any(FavouriteId.class));
    }

    @Test
    void shouldReturnBadRequestWhenSaveWithNullFavourite() throws Exception {
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(favouriteService, never()).save(any());
    }

    @Test
    void shouldReturnNotFoundWhenFavouriteDoesNotExist() throws Exception {
        // Mock service to throw exception
        when(favouriteService.findById(any(FavouriteId.class)))
                .thenThrow(new com.selimhorri.app.exception.wrapper.FavouriteNotFoundException(
                        "Favourite with userId: 1 and productId: 999 not found"));

        // Perform request and verify
        mockMvc.perform(get("/api/favourites/{userId}/{productId}", 1, 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(favouriteService, times(1)).findById(any(FavouriteId.class));
    }
}
