package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.OrderItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para operaciones CRUD de artículos de envío.
 * Expone endpoints para consultar, crear y eliminar artículos de pedidos
 * asociados a operaciones de envío y logística.
 */
@RestController
@RequestMapping("/api/shippings")
@Slf4j
@RequiredArgsConstructor
public class OrderItemResource {
	
	private final OrderItemService orderItemService;
	
	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene la lista completa de artículos de envío.
	 *
	 * @return ResponseEntity con lista de OrderItemDto envueltos en DtoCollectionResponse
	 */
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<OrderItemDto>> findAll() {
		log.info("Obteniendo lista completa de artículos de envío");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.orderItemService.findAll()));
	}
	
	/**
	 * Obtiene un artículo de envío específico por su ID.
	 *
	 * @param orderId ID del artículo a buscar
	 * @return ResponseEntity con OrderItemDto encontrado
	 */
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderItemDto> findById(
			@PathVariable("orderId") final String orderId) {
		log.info("Buscando artículo de envío con ID: {}", orderId);
		return ResponseEntity.ok(this.orderItemService.findById(Integer.parseInt(orderId)));
	}
	
	// ========== CREACIÓN ==========

	/**
	 * Crea un nuevo artículo de envío.
	 *
	 * @param orderItemDto Datos del artículo a crear (no puede ser nulo)
	 * @return ResponseEntity con OrderItemDto creado
	 */
	@PostMapping
	public ResponseEntity<OrderItemDto> save(
			@RequestBody 
			@NotNull(message = "El objeto de entrada no puede ser nulo") 
			@Valid final OrderItemDto orderItemDto) {
		log.info("Creando nuevo artículo de envío para orden ID: {}", orderItemDto.getOrderId());
		return ResponseEntity.ok(this.orderItemService.save(orderItemDto));
	}
	
	// ========== ELIMINACIÓN ==========

	/**
	 * Elimina un artículo de envío por su ID.
	 *
	 * @param orderId ID del artículo a eliminar
	 * @return ResponseEntity con true indicando éxito de la operación
	 */
	@DeleteMapping("/{orderId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("orderId") final String orderId) {
		log.info("Eliminando artículo de envío con ID: {}", orderId);
		this.orderItemService.deleteById(Integer.parseInt(orderId));
		return ResponseEntity.ok(true);
	}
	
}