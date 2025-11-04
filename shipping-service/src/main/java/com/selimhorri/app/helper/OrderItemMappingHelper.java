package com.selimhorri.app.helper;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;

/**
 * Utilidad de mapeo para conversión entre entidades OrderItem y DTOs.
 * Proporciona métodos estáticos para convertir entre objetos de dominio
 * y objetos de transferencia de datos (DTOs) de artículos de envío.
 */
public interface OrderItemMappingHelper {
	
	// ========== MAPEOS BÁSICOS ==========
	
	/**
	 * Convierte una entidad OrderItem a su DTO correspondiente.
	 * Enriquece el DTO con información básica de Product y Order.
	 *
	 * @param orderItem Entidad OrderItem a mapear
	 * @return OrderItemDto con datos mapeados
	 */
	public static OrderItemDto map(final OrderItem orderItem) {
		return OrderItemDto.builder()
				.productId(orderItem.getProductId())
				.orderId(orderItem.getOrderId())
				.orderedQuantity(orderItem.getOrderedQuantity())
				.productDto(
						ProductDto.builder()
							.productId(orderItem.getProductId())
							.build())
				.orderDto(
						OrderDto.builder()
							.orderId(orderItem.getOrderId())
							.build())
				.build();
	}
	
	/**
	 * Convierte un DTO OrderItemDto a su entidad correspondiente.
	 * Mapeo básico sin establecer campos de control como isActive.
	 *
	 * @param orderItemDto DTO OrderItemDto a mapear
	 * @return Entidad OrderItem con datos mapeados
	 */
	public static OrderItem map(final OrderItemDto orderItemDto) {
		return OrderItem.builder()
				.productId(orderItemDto.getProductId())
				.orderId(orderItemDto.getOrderId())
				.orderedQuantity(orderItemDto.getOrderedQuantity())
				.build();
	}
	
	// ========== MAPEOS ESPECIALIZADOS ==========
	
	/**
	 * Convierte un DTO OrderItemDto a entidad para operación de creación.
	 * Establece el estado isActive en true automáticamente.
	 *
	 * @param orderItemDto DTO OrderItemDto a mapear
	 * @return Entidad OrderItem con isActive=true
	 */
	public static OrderItem mapForCreation(final OrderItemDto orderItemDto) {
		return OrderItem.builder()
				.productId(orderItemDto.getProductId())
				.orderId(orderItemDto.getOrderId())
				.isActive(true)
				.orderedQuantity(orderItemDto.getOrderedQuantity())
				.build();
	}
	
}