package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Crear cart de prueba
        Cart testCart = new Cart();
        testCart.setCartId(101);
        
        // Crear orden de prueba con cart
        testOrder = new Order();
        testOrder.setOrderId(15);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setOrderDesc("Orden de prueba para laptop gaming");
        testOrder.setOrderFee(2500.00);
        testOrder.setStatus(OrderStatus.CREATED);
        testOrder.setActive(true);
        testOrder.setCart(testCart);
    }

    @Test
    void findAll_shouldReturnListOfActiveOrders() {
        when(orderRepository.findAllByIsActiveTrue()).thenReturn(List.of(testOrder));

        List<OrderDto> result = orderService.findAll();

        assertEquals(1, result.size());
        assertEquals(15, result.get(0).getOrderId());
        assertEquals("Orden de prueba para laptop gaming", result.get(0).getOrderDesc());
        verify(orderRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoActiveOrders() {
        when(orderRepository.findAllByIsActiveTrue()).thenReturn(List.of());

        List<OrderDto> result = orderService.findAll();

        assertTrue(result.isEmpty());
        verify(orderRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    void findById_shouldReturnOrderWhenFound() {
        when(orderRepository.findByOrderIdAndIsActiveTrue(15)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.findById(15);

        assertNotNull(result);
        assertEquals(15, result.getOrderId());
        assertEquals(2500.00, result.getOrderFee());
        assertEquals(OrderStatus.CREATED, result.getOrderStatus());
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(15);
    }

    @Test
    void findById_shouldThrowExceptionWhenNotFound() {
        when(orderRepository.findByOrderIdAndIsActiveTrue(99)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(
            OrderNotFoundException.class, 
            () -> orderService.findById(99)
        );
        
        assertTrue(exception.getMessage().contains("Orden activa con ID 99 no encontrada"));
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(99);
    }

    @Test
    void updateStatus_shouldTransitionFromCreatedToOrdered() {
        when(orderRepository.findByOrderIdAndIsActiveTrue(15)).thenReturn(Optional.of(testOrder));
        
        // Crear orden actualizada con Cart
        Cart testCart = new Cart();
        testCart.setCartId(101);
        
        Order updatedOrder = new Order();
        updatedOrder.setOrderId(15);
        updatedOrder.setStatus(OrderStatus.ORDERED);
        updatedOrder.setCart(testCart);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderDto result = orderService.updateStatus(15);

        assertNotNull(result);
        assertEquals(OrderStatus.ORDERED, result.getOrderStatus());
        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(15);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateStatus_shouldTransitionFromOrderedToInPayment() {
        testOrder.setStatus(OrderStatus.ORDERED);
        when(orderRepository.findByOrderIdAndIsActiveTrue(15)).thenReturn(Optional.of(testOrder));
        
        // Crear orden actualizada con Cart
        Cart testCart = new Cart();
        testCart.setCartId(101);
        
        Order updatedOrder = new Order();
        updatedOrder.setOrderId(15);
        updatedOrder.setStatus(OrderStatus.IN_PAYMENT);
        updatedOrder.setCart(testCart);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderDto result = orderService.updateStatus(15);

        assertNotNull(result);
        assertEquals(OrderStatus.IN_PAYMENT, result.getOrderStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateStatus_shouldThrowExceptionWhenOrderAlreadyInPayment() {
        testOrder.setStatus(OrderStatus.IN_PAYMENT);
        when(orderRepository.findByOrderIdAndIsActiveTrue(15)).thenReturn(Optional.of(testOrder));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> orderService.updateStatus(15)
        );
        
        assertTrue(exception.getMessage().contains("ya está PAGADA y no puede actualizarse más"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteById_shouldSoftDeleteOrder() {
        when(orderRepository.findByOrderIdAndIsActiveTrue(15)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.deleteById(15);

        verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(15);
        verify(orderRepository, times(1)).save(argThat(order -> !order.isActive()));
    }

    @Test
    void deleteById_shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findByOrderIdAndIsActiveTrue(99)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteById(99));
        verify(orderRepository, never()).save(any());
    }
}
