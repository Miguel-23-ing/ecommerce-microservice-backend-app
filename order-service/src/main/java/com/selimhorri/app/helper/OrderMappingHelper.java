package com.selimhorri.app.helper;

import java.time.LocalDateTime;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;

/**
 * Utilidad para mapeo entre entidades de Orden y DTOs.
 * Proporciona métodos estáticos para convertir entre:
 * - Order (entidad JPA) <-> OrderDto (DTO de transferencia)
 * 
 * Maneja la conversión de atributos, estados por defecto y asociaciones.
 */
public interface OrderMappingHelper {

	// ========== MAPEOS BÁSICOS ==========

	/**
	 * Convierte una entidad Order a OrderDto.
	 * Mapea todos los atributos incluyendo la relación con Cart.
	 *
	 * @param order Entidad Order a convertir
	 * @return OrderDto con datos de la orden
	 */
	public static OrderDto map(final Order order) {
		return OrderDto.builder()
						.orderId(order.getOrderId())
						.orderDate(order.getOrderDate())
						.orderDesc(order.getOrderDesc())
						.orderFee(order.getOrderFee())
						.orderStatus(order.getStatus())
						.cartDto(
										CartDto.builder()
										.cartId(order.getCart().getCartId())
										.build())
						.build();
	}

	/**
	 * Convierte un OrderDto a entidad Order.
	 * Establece la fecha actual como orderDate y aplica estado por defecto (CREATED).
	 *
	 * @param orderDto DTO de orden a convertir
	 * @return Entidad Order con los datos del DTO
	 */
	public static Order map(final OrderDto orderDto) {
		return Order.builder()
						.orderId(orderDto.getOrderId())
						.orderDate(LocalDateTime.now())
						.orderDesc(orderDto.getOrderDesc())
						.orderFee(orderDto.getOrderFee())
						.status(
										orderDto.getOrderStatus() != null
										? orderDto.getOrderStatus()
										: OrderStatus.CREATED)
						.cart(
										Cart.builder()
										.cartId(orderDto.getCartDto().getCartId())
										.build())
						.build();
	}

	// ========== MAPEOS ESPECIALIZADOS ==========

	/**
	 * Convierte un OrderDto a entidad Order para operaciones de creación.
	 * Inicializa isActive en true y establece estado por defecto (CREATED).
	 * Establece la fecha actual como orderDate.
	 *
	 * @param orderDto DTO de orden a convertir
	 * @return Entidad Order con flags de creación inicializados
	 */
	public static Order mapForCreationOrder(final OrderDto orderDto) {
		return Order.builder()
						.orderId(orderDto.getOrderId())
						.orderDate(LocalDateTime.now())
						.orderDesc(orderDto.getOrderDesc())
						.orderFee(orderDto.getOrderFee())
						.isActive(true)
						.status(
										orderDto.getOrderStatus() != null
										? orderDto.getOrderStatus()
										: OrderStatus.CREATED)
						.cart(
										Cart.builder()
										.cartId(orderDto.getCartDto().getCartId())
										.build())
						.build();
	}

	/**
	 * Convierte un OrderDto a entidad Order para operaciones de actualización.
	 * Preserva la asociación del carrito existente.
	 * Mantiene la fecha original del DTO si está disponible.
	 *
	 * @param orderDto DTO con datos actualizados
	 * @param cart Carrito existente a preservar en la asociación
	 * @return Entidad Order con datos actualizados y carrito preservado
	 */
	public static Order mapForUpdate(final OrderDto orderDto, final Cart cart) {
		return Order.builder()
						.orderId(orderDto.getOrderId())
						.orderDate(orderDto.getOrderDate())
						.orderDesc(orderDto.getOrderDesc())
						.orderFee(orderDto.getOrderFee())
						.cart(cart)
						.build();
	}
}