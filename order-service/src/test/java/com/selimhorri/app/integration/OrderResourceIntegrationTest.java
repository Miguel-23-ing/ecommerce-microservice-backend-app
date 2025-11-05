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
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.service.OrderService;

/**
 * Tests de integración para OrderResource (Controller).
 * Usa MockMvc para simular peticiones HTTP y verifica las respuestas.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class OrderResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldFetchAllOrders() throws Exception {
        // Mock data
        CartDto cart1 = CartDto.builder()
                .cartId(1)
                .build();

        OrderDto orderDto1 = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Orden de laptop gaming")
                .orderFee(2500.00)
                .orderStatus(OrderStatus.CREATED)
                .cartDto(cart1)
                .build();

        CartDto cart2 = CartDto.builder()
                .cartId(2)
                .build();

        OrderDto orderDto2 = OrderDto.builder()
                .orderId(2)
                .orderDate(LocalDateTime.now())
                .orderDesc("Orden de periféricos")
                .orderFee(500.00)
                .orderStatus(OrderStatus.ORDERED)
                .cartDto(cart2)
                .build();

        List<OrderDto> orderDtos = List.of(orderDto1, orderDto2);

        // Mock service call
        when(orderService.findAll()).thenReturn(orderDtos);

        // Perform request and verify
        mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                .andExpect(jsonPath("$.collection[0].orderDesc").value("Orden de laptop gaming"))
                .andExpect(jsonPath("$.collection[0].orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.collection[1].orderId").value(2))
                .andExpect(jsonPath("$.collection[1].orderStatus").value("ORDERED"));

        verify(orderService, times(1)).findAll();
    }

    @Test
    void shouldFetchOrderById() throws Exception {
        // Mock data
        CartDto cart = CartDto.builder()
                .cartId(1)
                .build();

        OrderDto orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Orden de laptop gaming")
                .orderFee(2500.00)
                .orderStatus(OrderStatus.CREATED)
                .cartDto(cart)
                .build();

        // Mock service call
        when(orderService.findById(anyInt())).thenReturn(orderDto);

        // Perform request and verify
        mockMvc.perform(get("/api/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("Orden de laptop gaming"))
                .andExpect(jsonPath("$.orderFee").value(2500.00))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"));

        verify(orderService, times(1)).findById(1);
    }

    @Test
    void shouldSaveOrder() throws Exception {
        // Mock data
        CartDto cart = CartDto.builder()
                .cartId(1)
                .build();

        OrderDto inputDto = OrderDto.builder()
                .orderDesc("Nueva orden")
                .orderFee(1500.00)
                .cartDto(cart)
                .build();

        OrderDto savedDto = OrderDto.builder()
                .orderId(3)
                .orderDate(LocalDateTime.now())
                .orderDesc("Nueva orden")
                .orderFee(1500.00)
                .orderStatus(OrderStatus.CREATED)
                .cartDto(cart)
                .build();

        // Mock service call
        when(orderService.save(any(OrderDto.class))).thenReturn(savedDto);

        // Perform request and verify
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(3))
                .andExpect(jsonPath("$.orderDesc").value("Nueva orden"))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"));

        verify(orderService, times(1)).save(any(OrderDto.class));
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        // Mock data
        CartDto cart = CartDto.builder()
                .cartId(1)
                .build();

        OrderDto updatedDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Orden de laptop gaming")
                .orderFee(2500.00)
                .orderStatus(OrderStatus.ORDERED)
                .cartDto(cart)
                .build();

        // Mock service call
        when(orderService.updateStatus(anyInt())).thenReturn(updatedDto);

        // Perform request and verify (endpoint: PATCH /api/orders/{orderId}/status)
        mockMvc.perform(patch("/api/orders/{orderId}/status", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderStatus").value("ORDERED"));

        verify(orderService, times(1)).updateStatus(1);
    }

    @Test
    void shouldDeleteOrder() throws Exception {
        // Mock service (deleteById is void)
        doNothing().when(orderService).deleteById(anyInt());

        // Perform request and verify
        mockMvc.perform(delete("/api/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService, times(1)).deleteById(1);
    }

    @Test
    void shouldReturnBadRequestWhenSaveWithNullOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).save(any());
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Mock service to throw exception
        when(orderService.findById(anyInt()))
                .thenThrow(new com.selimhorri.app.exception.wrapper.OrderNotFoundException("Orden activa con ID 999 no encontrada"));

        // Perform request and verify
        mockMvc.perform(get("/api/orders/{orderId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).findById(999);
    }
}
