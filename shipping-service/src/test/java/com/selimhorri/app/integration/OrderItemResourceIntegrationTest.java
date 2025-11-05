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
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.service.OrderItemService;

/**
 * Tests de integración para OrderItemResource (Controller).
 * Usa MockMvc para simular peticiones HTTP y verifica las respuestas.
 */
@Tag("integration")
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class OrderItemResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderItemService orderItemService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldFetchAllOrderItems() throws Exception {
        // Mock data
        ProductDto product1 = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop HP")
                .priceUnit(2500.00)
                .build();

        OrderDto order1 = OrderDto.builder()
                .orderId(1)
                .orderStatus("ORDERED")
                .build();

        OrderItemDto orderItemDto1 = OrderItemDto.builder()
                .orderId(1)
                .productId(1)
                .orderedQuantity(2)
                .productDto(product1)
                .orderDto(order1)
                .build();

        ProductDto product2 = ProductDto.builder()
                .productId(2)
                .productTitle("Mouse Logitech")
                .priceUnit(150.00)
                .build();

        OrderDto order2 = OrderDto.builder()
                .orderId(2)
                .orderStatus("PAID")
                .build();

        OrderItemDto orderItemDto2 = OrderItemDto.builder()
                .orderId(2)
                .productId(2)
                .orderedQuantity(5)
                .productDto(product2)
                .orderDto(order2)
                .build();

        List<OrderItemDto> orderItemDtos = List.of(orderItemDto1, orderItemDto2);

        // Mock service call
        when(orderItemService.findAll()).thenReturn(orderItemDtos);

        // Perform request and verify
        mockMvc.perform(get("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].orderedQuantity").value(2))
                .andExpect(jsonPath("$.collection[1].orderId").value(2))
                .andExpect(jsonPath("$.collection[1].productId").value(2));

        verify(orderItemService, times(1)).findAll();
    }

    @Test
    void shouldFetchOrderItemById() throws Exception {
        // Mock data
        ProductDto product = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop HP")
                .priceUnit(2500.00)
                .build();

        OrderDto order = OrderDto.builder()
                .orderId(1)
                .orderStatus("ORDERED")
                .build();

        OrderItemDto orderItemDto = OrderItemDto.builder()
                .orderId(1)
                .productId(1)
                .orderedQuantity(2)
                .productDto(product)
                .orderDto(order)
                .build();

        // Mock service call
        when(orderItemService.findById(anyInt())).thenReturn(orderItemDto);

        // Perform request and verify
        mockMvc.perform(get("/api/shippings/{orderItemId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.orderedQuantity").value(2))
                .andExpect(jsonPath("$.product.productTitle").value("Laptop HP"));

        verify(orderItemService, times(1)).findById(1);
    }

    @Test
    void shouldSaveOrderItem() throws Exception {
        // Mock data
        ProductDto product = ProductDto.builder()
                .productId(3)
                .productTitle("Teclado Mecánico")
                .priceUnit(350.00)
                .build();

        OrderDto order = OrderDto.builder()
                .orderId(3)
                .orderStatus("ORDERED")
                .build();

        OrderItemDto inputDto = OrderItemDto.builder()
                .orderId(3)
                .productId(3)
                .orderedQuantity(1)
                .build();

        OrderItemDto savedDto = OrderItemDto.builder()
                .orderId(3)
                .productId(3)
                .orderedQuantity(1)
                .productDto(product)
                .orderDto(order)
                .build();

        // Mock service call
        when(orderItemService.save(any(OrderItemDto.class))).thenReturn(savedDto);

        // Perform request and verify
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(3))
                .andExpect(jsonPath("$.productId").value(3))
                .andExpect(jsonPath("$.orderedQuantity").value(1));

        verify(orderItemService, times(1)).save(any(OrderItemDto.class));
    }

    @Test
    void shouldDeleteOrderItem() throws Exception {
        // Mock service (deleteById is void)
        doNothing().when(orderItemService).deleteById(anyInt());

        // Perform request and verify
        mockMvc.perform(delete("/api/shippings/{orderItemId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderItemService, times(1)).deleteById(1);
    }

    @Test
    void shouldReturnBadRequestWhenDeletingOrderItemWithWrongStatus() throws Exception {
        // Mock service to throw exception
        doThrow(new IllegalStateException("Cannot delete order item - associated order is not in ORDERED status"))
                .when(orderItemService).deleteById(anyInt());

        // Perform request and verify
        mockMvc.perform(delete("/api/shippings/{orderItemId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(orderItemService, times(1)).deleteById(1);
    }

    @Test
    void shouldReturnBadRequestWhenBlankOrderItemId() throws Exception {
        mockMvc.perform(get("/api/shippings/{orderItemId}", " ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(orderItemService, never()).findById(anyInt());
    }

    @Test
    void shouldReturnBadRequestWhenSaveWithNullOrderItem() throws Exception {
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(orderItemService, never()).save(any());
    }

    @Test
    void shouldReturnNotFoundWhenOrderItemDoesNotExist() throws Exception {
        // Mock service to throw exception
        when(orderItemService.findById(anyInt()))
                .thenThrow(new com.selimhorri.app.exception.wrapper.OrderItemNotFoundException("OrderItem with id: 999 not found"));

        // Perform request and verify
        mockMvc.perform(get("/api/shippings/{orderItemId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(orderItemService, times(1)).findById(999);
    }
}
