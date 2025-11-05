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
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.service.ProductService;

/**
 * Tests de integración para ProductResource (Controller).
 * Usa MockMvc para simular peticiones HTTP y verifica las respuestas.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class ProductResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldFetchAllProducts() throws Exception {
        // Mock data
        CategoryDto category = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electrónicos")
                .imageUrl("http://example.com/electronics.jpg")
                .build();

        ProductDto productDto1 = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop HP")
                .imageUrl("http://example.com/laptop.jpg")
                .sku("LAP-001")
                .priceUnit(2500.00)
                .quantity(10)
                .categoryDto(category)
                .build();

        ProductDto productDto2 = ProductDto.builder()
                .productId(2)
                .productTitle("Mouse Logitech")
                .imageUrl("http://example.com/mouse.jpg")
                .sku("MOU-002")
                .priceUnit(150.00)
                .quantity(50)
                .categoryDto(category)
                .build();

        List<ProductDto> productDtos = List.of(productDto1, productDto2);

        // Mock service call
        when(productService.findAll()).thenReturn(productDtos);

        // Perform request and verify
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].productTitle").value("Laptop HP"))
                .andExpect(jsonPath("$.collection[0].sku").value("LAP-001"))
                .andExpect(jsonPath("$.collection[1].productId").value(2))
                .andExpect(jsonPath("$.collection[1].productTitle").value("Mouse Logitech"));

        verify(productService, times(1)).findAll();
    }

    @Test
    void shouldFetchProductById() throws Exception {
        // Mock data
        CategoryDto category = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electrónicos")
                .imageUrl("http://example.com/electronics.jpg")
                .build();

        ProductDto productDto = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop HP")
                .imageUrl("http://example.com/laptop.jpg")
                .sku("LAP-001")
                .priceUnit(2500.00)
                .quantity(10)
                .categoryDto(category)
                .build();

        // Mock service call
        when(productService.findById(anyInt())).thenReturn(productDto);

        // Perform request and verify
        mockMvc.perform(get("/api/products/{productId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Laptop HP"))
                .andExpect(jsonPath("$.sku").value("LAP-001"))
                .andExpect(jsonPath("$.priceUnit").value(2500.00))
                .andExpect(jsonPath("$.category.categoryTitle").value("Electrónicos"));

        verify(productService, times(1)).findById(1);
    }

    @Test
    void shouldSaveProduct() throws Exception {
        // Mock data
        CategoryDto category = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electrónicos")
                .build();

        ProductDto inputDto = ProductDto.builder()
                .productTitle("Teclado Mecánico")
                .imageUrl("http://example.com/keyboard.jpg")
                .sku("KEY-003")
                .priceUnit(350.00)
                .quantity(25)
                .categoryDto(category)
                .build();

        ProductDto savedDto = ProductDto.builder()
                .productId(3)
                .productTitle("Teclado Mecánico")
                .imageUrl("http://example.com/keyboard.jpg")
                .sku("KEY-003")
                .priceUnit(350.00)
                .quantity(25)
                .categoryDto(category)
                .build();

        // Mock service call
        when(productService.save(any(ProductDto.class))).thenReturn(savedDto);

        // Perform request and verify
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(3))
                .andExpect(jsonPath("$.productTitle").value("Teclado Mecánico"))
                .andExpect(jsonPath("$.sku").value("KEY-003"));

        verify(productService, times(1)).save(any(ProductDto.class));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        // Mock data
        CategoryDto category = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electrónicos")
                .build();

        ProductDto inputDto = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop HP Actualizada")
                .imageUrl("http://example.com/laptop-new.jpg")
                .sku("LAP-001")
                .priceUnit(2700.00)
                .quantity(8)
                .categoryDto(category)
                .build();

        // Mock service call
        when(productService.update(any(ProductDto.class))).thenReturn(inputDto);

        // Perform request and verify
        mockMvc.perform(put("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Laptop HP Actualizada"))
                .andExpect(jsonPath("$.priceUnit").value(2700.00));

        verify(productService, times(1)).update(any(ProductDto.class));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        // Mock service (deleteById is void)
        doNothing().when(productService).deleteById(anyInt());

        // Perform request and verify
        mockMvc.perform(delete("/api/products/{productId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(productService, times(1)).deleteById(1);
    }

    @Test
    void shouldReturnBadRequestWhenBlankProductId() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", " ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(productService, never()).findById(anyInt());
    }

    @Test
    void shouldReturnBadRequestWhenSaveWithNullProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestWhenUpdateWithNullProduct() throws Exception {
        mockMvc.perform(put("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).update(any());
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        // Mock service to throw exception
        when(productService.findById(anyInt()))
                .thenThrow(new com.selimhorri.app.exception.wrapper.ProductNotFoundException("Product with id: 999 not found"));

        // Perform request and verify
        mockMvc.perform(get("/api/products/{productId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(productService, times(1)).findById(999);
    }
}
