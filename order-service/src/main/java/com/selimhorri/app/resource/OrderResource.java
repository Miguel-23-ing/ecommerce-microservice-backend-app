package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para operaciones CRUD de órdenes.
 * Proporciona endpoints para consultar, crear, actualizar estado y datos, y eliminar órdenes.
 * Valida automáticamente los payloads de entrada mediante anotaciones JSR-303.
 */
@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderResource {

	private final OrderService orderService;

	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene la lista completa de órdenes activas.
	 *
	 * @return ResponseEntity con lista de todas las órdenes activas
	 */
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<OrderDto>> findAll() {
		log.info("Obteniendo lista completa de órdenes");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.orderService.findAll()));
	}

	/**
	 * Obtiene una orden específica por su ID.
	 *
	 * @param orderId ID de la orden a buscar (no puede estar vacío)
	 * @return ResponseEntity con los datos de la orden
	 */
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderDto> findById(
			@PathVariable("orderId") @NotBlank(message = "El ID de la orden no puede estar vacío") @Valid final String orderId) {
		log.info("Buscando orden con ID: {}", orderId);
		return ResponseEntity.ok(this.orderService.findById(Integer.parseInt(orderId)));
	}

	// ========== CREACIÓN ==========

	/**
	 * Crea una nueva orden.
	 *
	 * @param orderDto Datos de la nueva orden a crear (requiere carrito asociado)
	 * @return ResponseEntity con los datos de la orden creada
	 */
	@PostMapping
	public ResponseEntity<OrderDto> save(
			@RequestBody @NotNull(message = "El payload de la orden no puede ser nulo") @Valid final OrderDto orderDto) {
		log.info("Creando nueva orden con descripción: {}", orderDto.getOrderDesc());
		return ResponseEntity.ok(this.orderService.save(orderDto));
	}

	// ========== ACTUALIZACIÓN DE ESTADO ==========

	/**
	 * Actualiza el estado de una orden siguiendo la transición de estados.
	 * Secuencia: CREATED -> ORDERED -> IN_PAYMENT (terminal)
	 *
	 * @param orderId ID de la orden cuyo estado será actualizado
	 * @return ResponseEntity con la orden con estado actualizado
	 */
	@PatchMapping("/{orderId}/status")
	public ResponseEntity<OrderDto> updateStatus(
			@PathVariable("orderId") @NotBlank(message = "El ID de la orden no puede estar vacío") @Valid final int orderId) {
		log.info("Actualizando estado de orden con ID: {}", orderId);
		return ResponseEntity.ok(this.orderService.updateStatus(orderId));
	}

	// ========== ACTUALIZACIÓN DE DATOS ==========

	/**
	 * Actualiza los datos de una orden preservando su estado y carrito.
	 * Solo actualiza descripción y tarifa.
	 *
	 * @param orderId ID de la orden a actualizar
	 * @param orderDto Datos nuevos para la orden
	 * @return ResponseEntity con los datos de la orden actualizada
	 */
	@PutMapping("/{orderId}")
	public ResponseEntity<OrderDto> update(
			@PathVariable("orderId") @NotBlank(message = "El ID de la orden no puede estar vacío") @Valid final String orderId,
			@RequestBody @NotNull(message = "El payload de la orden no puede ser nulo") @Valid final OrderDto orderDto) {
		log.info("Actualizando orden con ID: {}", orderId);
		return ResponseEntity.ok(this.orderService.update(Integer.parseInt(orderId), orderDto));
	}

	// ========== ELIMINACIÓN ==========

	/**
	 * Elimina una orden existente por su ID (soft delete).
	 * Solo permite eliminar órdenes en estado CREATED u ORDERED.
	 *
	 * @param orderId ID de la orden a eliminar
	 * @return ResponseEntity con estado de éxito
	 */
	@DeleteMapping("/{orderId}")
	public ResponseEntity<Boolean> deleteById(@PathVariable("orderId") final String orderId) {
		log.info("Eliminando orden con ID: {}", orderId);
		this.orderService.deleteById(Integer.parseInt(orderId));
		return ResponseEntity.ok(true);
	}

}
