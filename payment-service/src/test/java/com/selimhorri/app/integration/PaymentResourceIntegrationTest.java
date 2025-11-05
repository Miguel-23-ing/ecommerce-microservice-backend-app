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
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.service.PaymentService;

/**
 * Tests de integración para PaymentResource (Controller).
 * Usa MockMvc para simular peticiones HTTP y verifica las respuestas.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class PaymentResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldFetchAllPayments() throws Exception {
        // Mock data
        OrderDto order1 = OrderDto.builder()
                .orderId(1)
                .orderStatus("IN_PAYMENT")
                .build();

        OrderDto order2 = OrderDto.builder()
                .orderId(2)
                .orderStatus("IN_PAYMENT")
                .build();

        PaymentDto payment1 = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(order1)
                .build();

        PaymentDto payment2 = PaymentDto.builder()
                .paymentId(2)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(order2)
                .build();

        List<PaymentDto> payments = List.of(payment1, payment2);

        // Mock service call
        when(paymentService.findAll()).thenReturn(payments);

        // Perform request and verify
        mockMvc.perform(get("/api/payments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].paymentId").value(1))
                .andExpect(jsonPath("$.collection[1].paymentId").value(2));

        verify(paymentService, times(1)).findAll();
    }

    @Test
    void shouldFetchPaymentById() throws Exception {
        // Mock data
        OrderDto order = OrderDto.builder()
                .orderId(1)
                .orderStatus("IN_PAYMENT")
                .orderDate(LocalDateTime.now())
                .build();

        PaymentDto payment = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(order)
                .build();

        // Mock service call
        when(paymentService.findById(1)).thenReturn(payment);

        // Perform request and verify
        mockMvc.perform(get("/api/payments/{paymentId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(false))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_STARTED"));

        verify(paymentService, times(1)).findById(1);
    }

    @Test
    void shouldCreatePayment() throws Exception {
        // Mock data
        OrderDto order = OrderDto.builder()
                .orderId(3)
                .orderStatus("IN_PAYMENT")
                .build();

        PaymentDto inputDto = PaymentDto.builder()
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(order)
                .build();

        PaymentDto createdDto = PaymentDto.builder()
                .paymentId(3)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(order)
                .build();

        // Mock service call
        when(paymentService.save(any(PaymentDto.class))).thenReturn(createdDto);

        // Perform request and verify
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(3))
                .andExpect(jsonPath("$.isPayed").value(false))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_STARTED"));

        verify(paymentService, times(1)).save(any(PaymentDto.class));
    }

    @Test
    void shouldUpdatePaymentStatusWithPatch() throws Exception {
        // Mock data
        OrderDto order = OrderDto.builder()
                .orderId(1)
                .orderStatus("IN_PAYMENT")
                .build();

        PaymentDto updatedPayment = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(order)
                .build();

        // Mock service call
        when(paymentService.updateStatus(1)).thenReturn(updatedPayment);

        // Perform request and verify
        mockMvc.perform(patch("/api/payments/{paymentId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.paymentStatus").value("IN_PROGRESS"));

        verify(paymentService, times(1)).updateStatus(1);
    }

    @Test
    void shouldUpdatePaymentStatusWithPut() throws Exception {
        // Mock data
        OrderDto order = OrderDto.builder()
                .orderId(1)
                .orderStatus("IN_PAYMENT")
                .build();

        PaymentDto updatedPayment = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(order)
                .build();

        // Mock service call
        when(paymentService.updateStatus(1)).thenReturn(updatedPayment);

        // Perform request and verify
        mockMvc.perform(put("/api/payments/{paymentId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));

        verify(paymentService, times(1)).updateStatus(1);
    }

    @Test
    void shouldDeletePayment() throws Exception {
        // Mock service (deleteById is void)
        doNothing().when(paymentService).deleteById(anyInt());

        // Perform request and verify
        mockMvc.perform(delete("/api/payments/{paymentId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(paymentService, times(1)).deleteById(1);
    }

    @Test
    void shouldReturnBadRequestWhenSaveWithNullPayment() throws Exception {
        // Perform request with null payload and verify
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).save(any(PaymentDto.class));
    }

    @Test
    void shouldHandleInvalidPaymentIdFormat() throws Exception {
        // Perform request with invalid ID and verify (NumberFormatException causes 400 Bad Request)
        mockMvc.perform(get("/api/payments/{paymentId}", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        verify(paymentService, never()).findById(anyInt());
    }
}
