package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para operaciones CRUD de carritos.
 * Proporciona endpoints para consultar, crear y eliminar carritos.
 * Valida automáticamente los payloads de entrada mediante anotaciones JSR-303.
 */
@RestController
@RequestMapping("/api/carts")
@Slf4j
@RequiredArgsConstructor
public class CartResource {
	
	private final CartService cartService;
	
	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene la lista completa de carritos.
	 *
	 * @return ResponseEntity con lista de todos los carritos
	 */
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<CartDto>> findAll() {
		log.info("Obteniendo lista completa de carritos");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.cartService.findAll()));
	}
	
	/**
	 * Obtiene un carrito específico por su ID.
	 *
	 * @param cartId ID del carrito a buscar (no puede estar vacío)
	 * @return ResponseEntity con los datos del carrito
	 */
	@GetMapping("/{cartId}")
	public ResponseEntity<CartDto> findById(
			@PathVariable("cartId") 
			@NotBlank(message = "El ID del carrito no puede estar vacío") 
			@Valid final String cartId) {
		log.info("Buscando carrito con ID: {}", cartId);
		return ResponseEntity.ok(this.cartService.findById(Integer.parseInt(cartId)));
	}
	
	// ========== CREACIÓN ==========

	/**
	 * Crea un nuevo carrito.
	 *
	 * @param cartDto Datos del nuevo carrito a crear
	 * @return ResponseEntity con los datos del carrito creado
	 */
	@PostMapping
	public ResponseEntity<CartDto> save(
			@RequestBody 
			@NotNull(message = "El payload del carrito no puede ser nulo") 
			@Valid final CartDto cartDto) {
		log.info("Creando nuevo carrito para usuario ID: {}", cartDto.getUserId());
		return ResponseEntity.ok(this.cartService.save(cartDto));
	}
	
	// ========== ELIMINACIÓN ==========

	/**
	 * Elimina un carrito existente por su ID.
	 *
	 * @param cartId ID del carrito a eliminar
	 * @return ResponseEntity con estado de éxito
	 */
	@DeleteMapping("/{cartId}")
	public ResponseEntity<Boolean> deleteById(@PathVariable("cartId") final String cartId) {
		log.info("Eliminando carrito con ID: {}", cartId);
		this.cartService.deleteById(Integer.parseInt(cartId));
		return ResponseEntity.ok(true);
	}
}