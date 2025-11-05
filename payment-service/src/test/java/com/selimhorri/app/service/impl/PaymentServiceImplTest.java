package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.exception.wrapper.PaymentServiceException;
import com.selimhorri.app.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private PaymentDto paymentDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        orderDto = OrderDto.builder()
                .orderId(5)
                .orderDate(LocalDateTime.now())
                .orderDesc("Pedido de prueba para laptop")
                .orderFee(1500.99)
                .orderStatus(OrderStatus.ORDERED.name())
                .build();

        payment = new Payment();
        payment.setPaymentId(10);
        payment.setIsPayed(false);
        payment.setPaymentStatus(PaymentStatus.NOT_STARTED);
        payment.setOrderId(5);

        paymentDto = PaymentDto.builder()
                .paymentId(10)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(orderDto)
                .build();
    }

    @Test
    void findAll_ShouldReturnPaymentsWithInPaymentOrderStatus() {
        // Given
        OrderDto inPaymentOrder = OrderDto.builder()
                .orderId(5)
                .orderStatus("IN_PAYMENT")
                .build();

        List<Payment> payments = Arrays.asList(payment);
        
        when(paymentRepository.findAll()).thenReturn(payments);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(inPaymentOrder);

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("IN_PAYMENT", result.get(0).getOrderDto().getOrderStatus());
        verify(paymentRepository).findAll();
        verify(restTemplate).getForObject(contains("/5"), eq(OrderDto.class));
    }

    @Test
    void findAll_ShouldFilterOutPaymentsWithoutInPaymentStatus() {
        // Given
        OrderDto orderedOrder = OrderDto.builder()
                .orderId(5)
                .orderStatus("ORDERED")
                .build();

        List<Payment> payments = Arrays.asList(payment);
        
        when(paymentRepository.findAll()).thenReturn(payments);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(orderedOrder);

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(paymentRepository).findAll();
    }

    @Test
    void findAll_ShouldHandleRestTemplateException() {
        // Given
        List<Payment> payments = Arrays.asList(payment);
        
        when(paymentRepository.findAll()).thenReturn(payments);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenThrow(new RestClientException("Servicio no disponible"));

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(paymentRepository).findAll();
    }

    @Test
    void findById_ShouldReturnPaymentWithOrderData() {
        // Given
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(orderDto);

        // When
        PaymentDto result = paymentService.findById(10);

        // Then
        assertNotNull(result);
        assertEquals(10, result.getPaymentId());
        assertNotNull(result.getOrderDto());
        assertEquals(5, result.getOrderDto().getOrderId());
        verify(paymentRepository).findById(10);
        verify(restTemplate).getForObject(contains("/5"), eq(OrderDto.class));
    }

    @Test
    void findById_ShouldThrowExceptionWhenPaymentNotFound() {
        // Given
        when(paymentRepository.findById(10)).thenReturn(Optional.empty());

        // When & Then
        PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentService.findById(10)
        );
        
        assertTrue(exception.getMessage().contains("Pago con ID 10 no encontrado"));
        verify(paymentRepository).findById(10);
        verify(restTemplate, never()).getForObject(anyString(), eq(OrderDto.class));
    }

    @Test
    void findById_ShouldThrowExceptionWhenOrderServiceFails() {
        // Given
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenThrow(new RestClientException("Servicio no disponible"));

        // When & Then
        PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentService.findById(10)
        );
        
        assertEquals("No se pudo obtener información de la orden para el pago", exception.getMessage());
        verify(paymentRepository).findById(10);
    }

    @Test
    void save_ShouldThrowExceptionWhenOrderIdIsNull() {
        // Given
        PaymentDto invalidPaymentDto = PaymentDto.builder()
                .paymentId(10)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(null)
                .build();

        // When & Then
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> paymentService.save(invalidPaymentDto)
        );
        
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void save_ShouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(null);

        // When & Then
        PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentService.save(paymentDto)
        );
        
        assertTrue(exception.getMessage().contains("Orden con ID 5 no encontrada"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void save_ShouldThrowExceptionWhenOrderStatusIsNotOrdered() {
        // Given
        OrderDto inPaymentOrder = OrderDto.builder()
                .orderId(5)
                .orderStatus(OrderStatus.IN_PAYMENT.name())
                .build();

        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(inPaymentOrder);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.save(paymentDto)
        );
        
        assertTrue(exception.getMessage().contains("No se puede procesar pago de una orden que no está en estado ORDERED"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updateStatus_ShouldUpdateFromNotStartedToInProgress() {
        // Given
        Payment updatedPayment = new Payment();
        updatedPayment.setPaymentId(10);
        updatedPayment.setPaymentStatus(PaymentStatus.IN_PROGRESS);

        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(updatedPayment);

        // When
        PaymentDto result = paymentService.updateStatus(10);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.IN_PROGRESS, result.getPaymentStatus());
        verify(paymentRepository).findById(10);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void updateStatus_ShouldUpdateFromInProgressToCompleted() {
        // Given
        payment.setPaymentStatus(PaymentStatus.IN_PROGRESS);
        Payment updatedPayment = new Payment();
        updatedPayment.setPaymentId(10);
        updatedPayment.setPaymentStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(updatedPayment);

        // When
        PaymentDto result = paymentService.updateStatus(10);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void updateStatus_ShouldThrowExceptionWhenPaymentCompleted() {
        // Given
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> paymentService.updateStatus(10)
        );
        
        assertTrue(exception.getMessage().contains("Pago ya está COMPLETADO"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updateStatus_ShouldThrowExceptionWhenPaymentNotFound() {
        // Given
        when(paymentRepository.findById(10)).thenReturn(Optional.empty());

        // When & Then
        PaymentNotFoundException exception = assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.updateStatus(10)
        );
        
        assertTrue(exception.getMessage().contains("Pago con ID 10 no encontrado"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void deleteById_ShouldCancelPayment() {
        // Given
        when(paymentRepository.findById(10)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        paymentService.deleteById(10);

        // Then
        verify(paymentRepository).findById(10);
        verify(paymentRepository).save(argThat(p -> p.getPaymentStatus() == PaymentStatus.CANCELED));
    }

    @Test
    void deleteById_ShouldThrowExceptionWhenPaymentNotFound() {
        // Given
        when(paymentRepository.findById(10)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.deleteById(10)
        );
        
        assertTrue(exception.getMessage().contains("Pago con ID 10 no encontrado"));
        verify(paymentRepository, never()).save(any());
    }
}
