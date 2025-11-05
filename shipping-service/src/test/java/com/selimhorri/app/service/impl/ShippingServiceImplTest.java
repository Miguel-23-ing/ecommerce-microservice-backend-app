package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderStatus;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.repository.OrderItemRepository;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private OrderItem testOrderItem;
    private ProductDto productDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        productDto = ProductDto.builder()
                .productId(10)
                .productTitle("Teclado Mecánico RGB")
                .priceUnit(129.99)
                .build();

        orderDto = OrderDto.builder()
                .orderId(5)
                .orderStatus(OrderStatus.ORDERED.name())
                .orderDesc("Orden de prueba")
                .build();

        testOrderItem = new OrderItem();
        testOrderItem.setOrderedQuantity(2);
        testOrderItem.setProductId(10);
        testOrderItem.setOrderId(5);
        testOrderItem.setActive(true);
    }

    @Test
    void findAll_shouldReturnListOfActiveOrderItems() {
        when(orderItemRepository.findByIsActiveTrue()).thenReturn(List.of(testOrderItem));
        when(restTemplate.getForObject(anyString(), eq(ProductDto.class)))
                .thenReturn(productDto);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(orderDto);

        List<OrderItemDto> result = orderItemService.findAll();

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getProductId());
        assertEquals(5, result.get(0).getOrderId());
        assertEquals(2, result.get(0).getOrderedQuantity());
        verify(orderItemRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void findAll_shouldFilterOutItemsWithNonOrderedStatus() {
        OrderDto paidOrder = OrderDto.builder()
                .orderId(5)
                .orderStatus(OrderStatus.PAID.name())
                .build();

        when(orderItemRepository.findByIsActiveTrue()).thenReturn(List.of(testOrderItem));
        when(restTemplate.getForObject(contains("product-service"), eq(ProductDto.class)))
                .thenReturn(productDto);
        when(restTemplate.getForObject(contains("order-service"), eq(OrderDto.class)))
                .thenReturn(paidOrder);

        List<OrderItemDto> result = orderItemService.findAll();

        assertTrue(result.isEmpty());
        verify(orderItemRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void findById_shouldReturnOrderItemWhenFound() {
        when(orderItemRepository.findById(5)).thenReturn(Optional.of(testOrderItem));
        when(restTemplate.getForObject(anyString(), eq(ProductDto.class)))
                .thenReturn(productDto);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class)))
                .thenReturn(orderDto);

        OrderItemDto result = orderItemService.findById(5);

        assertNotNull(result);
        assertEquals(5, result.getOrderId());
        assertEquals(10, result.getProductId());
        assertEquals(2, result.getOrderedQuantity());
        assertNotNull(result.getProductDto());
        assertNotNull(result.getOrderDto());
        verify(orderItemRepository, times(1)).findById(5);
    }

    @Test
    void findById_shouldThrowExceptionWhenNotFound() {
        when(orderItemRepository.findById(99)).thenReturn(Optional.empty());

        OrderItemNotFoundException exception = assertThrows(
            OrderItemNotFoundException.class,
            () -> orderItemService.findById(99)
        );

        assertTrue(exception.getMessage().contains("OrderItem with id: 99 not found"));
        verify(orderItemRepository, times(1)).findById(99);
    }

    @Test
    void deleteById_shouldSoftDeleteOrderItem() {
        // Mock repository - el método usa findByOrderIdAndIsActiveTrue (busca por orderId)
        when(orderItemRepository.findByOrderIdAndIsActiveTrue(5)).thenReturn(Optional.of(testOrderItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);
        
        // Mock RestTemplate para orden (OrderDto usa String para orderStatus)
        OrderDto orderDto = OrderDto.builder()
            .orderId(5)
            .orderStatus(OrderStatus.ORDERED.name())
            .build();
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(orderDto);

        orderItemService.deleteById(5);

        verify(orderItemRepository, times(1)).findByOrderIdAndIsActiveTrue(5);
        verify(orderItemRepository, times(1)).save(argThat(item -> !item.isActive()));
    }

    @Test
    void deleteById_shouldThrowExceptionWhenOrderItemNotFound() {
        when(orderItemRepository.findByOrderIdAndIsActiveTrue(99)).thenReturn(Optional.empty());

        OrderItemNotFoundException exception = assertThrows(
            OrderItemNotFoundException.class,
            () -> orderItemService.deleteById(99)
        );
        
        assertTrue(exception.getMessage().contains("OrderItem with id: 99 not found"));
        verify(orderItemRepository, times(1)).findByOrderIdAndIsActiveTrue(99);
        verify(orderItemRepository, never()).save(any());
    }
}
