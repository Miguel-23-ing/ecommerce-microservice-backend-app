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

import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.AddressService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller para la gestión de direcciones de usuarios.
 * Proporciona endpoints para obtener, crear, actualizar y eliminar direcciones.
 * Expone los datos en formato JSON en la ruta base /api/address.
 * 
 * @author Sistema de Gestión de Usuarios
 * @version 1.0
 */
@RestController
@RequestMapping(value = {"/api/address"})
@Slf4j
@RequiredArgsConstructor
public class AddressResource {
	
	private final AddressService addressService;

	// ============================================================================
	// BÚSQUEDA - Endpoints para obtener direcciones
	// ============================================================================

	/**
	 * Obtiene la lista completa de todas las direcciones del sistema.
	 * 
	 * @return ResponseEntity con lista de todas las direcciones
	 */
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<AddressDto>> findAll() {
		log.info("Obteniendo lista completa de direcciones");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.addressService.findAll()));
	}
	
	/**
	 * Obtiene una dirección específica por su identificador.
	 * 
	 * @param addressId Identificador de la dirección (como parámetro de ruta)
	 * @return ResponseEntity con la dirección solicitada
	 * @throws AddressNotFoundException Si la dirección no existe
	 */
	@GetMapping("/{addressId}")
	public ResponseEntity<AddressDto> findById(
			@PathVariable("addressId") 
			@NotBlank(message = "El ID de dirección no debe estar vacío") 
			@Valid final String addressId) {
		log.info("Buscando dirección con ID: {}", addressId);
		return ResponseEntity.ok(this.addressService.findById(Integer.parseInt(addressId.strip())));
	}

	// ============================================================================
	// CREACIÓN - Endpoints para crear nuevas direcciones
	// ============================================================================

	/**
	 * Crea una nueva dirección en el sistema.
	 * 
	 * @param addressDto Datos de la dirección a crear (en el cuerpo de la solicitud)
	 * @return ResponseEntity con la dirección creada
	 */
	@PostMapping
	public ResponseEntity<AddressDto> save(
			@RequestBody 
			@NotNull(message = "Los datos de dirección no deben ser nulos") 
			@Valid final AddressDto addressDto) {
		log.info("Creando nueva dirección: {} - {}", addressDto.getFullAddress(), addressDto.getCity());
		return ResponseEntity.ok(this.addressService.save(addressDto));
	}

	// ============================================================================
	// ACTUALIZACIÓN - Endpoints para actualizar direcciones existentes
	// ============================================================================

	/**
	 * Actualiza una dirección existente usando el objeto AddressDto.
	 * 
	 * @param addressDto Datos de la dirección con actualizaciones (en el cuerpo de la solicitud)
	 * @return ResponseEntity con la dirección actualizada
	 * @throws AddressNotFoundException Si la dirección no existe
	 */
	@PutMapping
	public ResponseEntity<AddressDto> update(
			@RequestBody 
			@NotNull(message = "Los datos de dirección no deben ser nulos") 
			@Valid final AddressDto addressDto) {
		log.info("Actualizando dirección con ID: {}", addressDto.getAddressId());
		return ResponseEntity.ok(this.addressService.update(addressDto));
	}
	
	/**
	 * Actualiza una dirección existente usando su identificador como parámetro de ruta.
	 * 
	 * @param addressId Identificador de la dirección (como parámetro de ruta)
	 * @param addressDto Datos con las actualizaciones (en el cuerpo de la solicitud)
	 * @return ResponseEntity con la dirección actualizada
	 * @throws AddressNotFoundException Si la dirección no existe
	 */
	@PutMapping("/{addressId}")
	public ResponseEntity<AddressDto> update(
			@PathVariable("addressId") 
			@NotBlank(message = "El ID de dirección no debe estar vacío") final String addressId, 
			@RequestBody 
			@NotNull(message = "Los datos de dirección no deben ser nulos") 
			@Valid final AddressDto addressDto) {
		log.info("Actualizando dirección con ID desde parámetro: {}", addressId);
		return ResponseEntity.ok(this.addressService.update(Integer.parseInt(addressId.strip()), addressDto));
	}

	// ============================================================================
	// ELIMINACIÓN - Endpoints para eliminar direcciones
	// ============================================================================

	/**
	 * Elimina una dirección del sistema por su identificador.
	 * 
	 * @param addressId Identificador de la dirección (como parámetro de ruta)
	 * @return ResponseEntity indicando éxito de la operación
	 */
	@DeleteMapping("/{addressId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("addressId") 
			@NotBlank(message = "El ID de dirección no debe estar vacío") 
			@Valid final String addressId) {
		log.info("Eliminando dirección con ID: {}", addressId);
		this.addressService.deleteById(Integer.parseInt(addressId));
		log.info("Dirección eliminada exitosamente");
		return ResponseEntity.ok(true);
	}	
}