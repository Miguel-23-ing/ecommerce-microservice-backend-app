package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de órdenes.
 * Gestiona la lógica de negocio para operaciones CRUD de órdenes,
 * incluyendo consultas, creación, actualización de estado y eliminación (soft delete).
 * Valida integridad referencial con carritos y controla transiciones de estado.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final CartRepository cartRepository;

	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene todas las órdenes activas de la base de datos.
	 * Convierte las entidades a DTOs y elimina duplicados.
	 *
	 * @return Lista de órdenes activas como DTOs
	 */
	@Override
	public List<OrderDto> findAll() {
		log.info("Obteniendo lista completa de órdenes activas desde la base de datos");
		return this.orderRepository.findAllByIsActiveTrue()
						.stream()
						.map(OrderMappingHelper::map)
						.distinct()
						.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Obtiene una orden activa específica por su ID.
	 * Lanza excepción si la orden no existe o no está activa.
	 *
	 * @param orderId ID de la orden a buscar
	 * @return Orden encontrada como DTO
	 * @throws OrderNotFoundException Si la orden activa no existe
	 */
	@Override
	public OrderDto findById(final Integer orderId) {
		log.info("Buscando orden activa con ID: {}", orderId);
		return this.orderRepository.findByOrderIdAndIsActiveTrue(orderId)
						.map(OrderMappingHelper::map)
						.orElseThrow(() -> new OrderNotFoundException(
								String.format("Orden activa con ID %d no encontrada", orderId)));
	}

	// ========== CREACIÓN ==========

	/**
	 * Crea una nueva orden en la base de datos.
	 * Valida que la orden esté asociada a un carrito existente.
	 * Establece estado inicial CREATED y activa.
	 *
	 * @param orderDto Datos de la nueva orden con carrito requerido
	 * @return Orden creada como DTO
	 * @throws IllegalArgumentException Si la orden no tiene carrito asociado
	 * @throws CartNotFoundException Si el carrito no existe
	 */
	@Override
	public OrderDto save(final OrderDto orderDto) {
		log.info("Guardando nueva orden con descripción: {}", orderDto.getOrderDesc());
		orderDto.setOrderId(null);
		orderDto.setOrderStatus(null);

		validateOrderHasCart(orderDto);
		validateCartExists(orderDto.getCartDto().getCartId());

		return OrderMappingHelper.map(
						this.orderRepository.save(OrderMappingHelper.mapForCreationOrder(orderDto)));
	}

	// ========== ACTUALIZACIÓN DE ESTADO ==========

	/**
	 * Actualiza el estado de una orden siguiendo la transición de estados permitida:
	 * CREATED -> ORDERED -> IN_PAYMENT (final)
	 * 
	 * @param orderId ID de la orden a actualizar
	 * @return Orden con estado actualizado como DTO
	 * @throws OrderNotFoundException Si la orden activa no existe
	 * @throws IllegalStateException Si la orden ya está en estado terminal (IN_PAYMENT)
	 */
	@Override
	public OrderDto updateStatus(final int orderId) {
		log.info("Actualizando estado de orden con ID: {}", orderId);
		try {
			Order existingOrder = this.orderRepository
							.findByOrderIdAndIsActiveTrue(orderId)
							.orElseThrow(() -> new OrderNotFoundException(
									"Orden con ID " + orderId + " no encontrada"));

			OrderStatus newStatus = determineNextOrderStatus(existingOrder.getStatus(), orderId);
			existingOrder.setStatus(newStatus);
			Order updatedOrder = this.orderRepository.save(existingOrder);

			log.info("Estado de orden {} actualizado de {} a {}", orderId, existingOrder.getStatus(), newStatus);
			return OrderMappingHelper.map(updatedOrder);

		} catch (Exception e) {
			log.error("Error actualizando estado de orden con ID: {}", orderId, e);
			throw e;
		}
	}

	// ========== ACTUALIZACIÓN DE DATOS ==========

	/**
	 * Actualiza los datos de una orden preservando su carrito asociado y fecha original.
	 * Solo actualiza descripción y tarifa.
	 *
	 * @param orderId ID de la orden a actualizar
	 * @param orderDto Datos nuevos para la orden
	 * @return Orden actualizada como DTO
	 * @throws OrderNotFoundException Si la orden activa no existe
	 */
	@Override
	public OrderDto update(final Integer orderId, final OrderDto orderDto) {
		log.info("Actualizando orden con ID: {}", orderId);
		orderDto.setOrderStatus(null);

		Order existingOrder = this.orderRepository.findByOrderIdAndIsActiveTrue(orderId)
						.orElseThrow(() -> new OrderNotFoundException("Orden con ID " + orderId + " no encontrada"));

		orderDto.setOrderId(orderId);
		orderDto.setOrderStatus(existingOrder.getStatus());
		Order updatedOrder = OrderMappingHelper.mapForUpdate(orderDto, existingOrder.getCart());
		updatedOrder.setOrderDate(existingOrder.getOrderDate());

		return OrderMappingHelper.map(this.orderRepository.save(updatedOrder));
	}

	// ========== ELIMINACIÓN ==========

	/**
	 * Elimina una orden de forma lógica (soft delete) estableciendo isActive en false.
	 * Solo permite eliminar órdenes en estado CREATED u ORDERED.
	 * Órdenes pagadas (IN_PAYMENT) no pueden ser eliminadas.
	 *
	 * @param orderId ID de la orden a eliminar
	 * @throws OrderNotFoundException Si la orden activa no existe
	 * @throws IllegalStateException Si la orden está en estado IN_PAYMENT
	 */
	@Override
	public void deleteById(final Integer orderId) {
		log.info("Eliminando orden con ID: {} (soft delete)", orderId);
		Order order = orderRepository.findByOrderIdAndIsActiveTrue(orderId)
						.orElseThrow(() -> new OrderNotFoundException("Orden con ID " + orderId + " no encontrada"));

		if (order.getStatus() == OrderStatus.IN_PAYMENT) {
			throw new IllegalStateException(
					"No se puede eliminar orden con ID " + orderId + " porque ya está PAGADA");
		}

		order.setActive(false);
		orderRepository.save(order);
		log.info("Orden con ID {} marcada como inactiva", orderId);
	}

	// ========== MÉTODOS AUXILIARES ==========

	/**
	 * Valida que una orden tenga carrito asociado.
	 *
	 * @param orderDto Orden a validar
	 * @throws IllegalArgumentException Si no tiene carrito
	 */
	private void validateOrderHasCart(OrderDto orderDto) {
		if (orderDto.getCartDto() == null || orderDto.getCartDto().getCartId() == null) {
			log.error("Intento de crear orden sin carrito asociado");
			throw new IllegalArgumentException("La orden debe estar asociada a un carrito");
		}
	}

	/**
	 * Valida que un carrito exista en la base de datos.
	 *
	 * @param cartId ID del carrito a validar
	 * @throws CartNotFoundException Si el carrito no existe
	 */
	private void validateCartExists(Integer cartId) {
		cartRepository.findById(cartId)
						.orElseThrow(() -> {
							log.error("Carrito no encontrado con ID: {}", cartId);
							return new CartNotFoundException("Carrito no encontrado con ID: " + cartId);
						});
	}

	/**
	 * Determina el siguiente estado válido en la secuencia de transición.
	 * Secuencia: CREATED -> ORDERED -> IN_PAYMENT (terminal)
	 *
	 * @param currentStatus Estado actual de la orden
	 * @param orderId ID de la orden (para logs)
	 * @return Próximo estado válido
	 * @throws IllegalStateException Si no hay transición válida
	 */
	private OrderStatus determineNextOrderStatus(OrderStatus currentStatus, int orderId) {
		switch (currentStatus) {
			case CREATED:
				return OrderStatus.ORDERED;
			case ORDERED:
				return OrderStatus.IN_PAYMENT;
			case IN_PAYMENT:
				throw new IllegalStateException(
						"Orden con ID " + orderId + " ya está PAGADA y no puede actualizarse más");
			default:
				throw new IllegalStateException("Estado desconocido: " + currentStatus);
		}
	}
}
