package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Address;
import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.exception.wrapper.AddressNotFoundException;
import com.selimhorri.app.helper.AddressMappingHelper;
import com.selimhorri.app.repository.AddressRepository;
import com.selimhorri.app.service.AddressService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de gestión de direcciones de usuarios.
 * Proporciona operaciones CRUD para direcciones, incluyendo búsqueda, creación,
 * actualización y eliminación de direcciones de entrega.
 * 
 * @author Sistema de Gestión de Usuarios
 * @version 1.0
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

	private final AddressRepository addressRepository;

	// ============================================================================
	// BÚSQUEDA - Métodos para obtener direcciones existentes
	// ============================================================================

	/**
	 * Obtiene la lista completa de todas las direcciones del sistema.
	 * 
	 * @return Lista inmutable de todos los AddressDto disponibles
	 */
	@Override
	public List<AddressDto> findAll() {
		log.info("Obteniendo lista completa de direcciones");
		return this.addressRepository.findAll()
				.stream()
				.map(AddressMappingHelper::map)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Busca una dirección específica por su identificador.
	 * 
	 * @param addressId Identificador único de la dirección
	 * @return AddressDto correspondiente al ID proporcionado
	 * @throws AddressNotFoundException Si la dirección no existe
	 */
	@Override
	public AddressDto findById(final Integer addressId) {
		log.info("Buscando dirección con ID: {}", addressId);
		return this.addressRepository.findById(addressId)
				.map(AddressMappingHelper::map)
				.orElseThrow(() -> new AddressNotFoundException(
						String.format("Dirección con ID: %d no encontrada", addressId)));
	}

	// ============================================================================
	// CREACIÓN - Métodos para crear nuevas direcciones
	// ============================================================================

	/**
	 * Crea una nueva dirección en el sistema.
	 * 
	 * @param addressDto Datos de la dirección a crear
	 * @return AddressDto creada y persistida
	 */
	@Override
	public AddressDto save(final AddressDto addressDto) {
		log.info("Creando nueva dirección: {} - {} - {}", addressDto.getFullAddress(), addressDto.getPostalCode(), addressDto.getCity());
		Address saved = this.addressRepository.save(AddressMappingHelper.map(addressDto));
		log.info("Dirección creada exitosamente con ID: {}", saved.getAddressId());
		return AddressMappingHelper.map(saved);
	}

	// ============================================================================
	// ACTUALIZACIÓN - Métodos para actualizar direcciones existentes
	// ============================================================================

	/**
	 * Actualiza una dirección existente usando el objeto AddressDto.
	 * Actualiza los campos: calle completa, código postal y ciudad.
	 * 
	 * @param addressDto Datos de la dirección con actualizaciones
	 * @return AddressDto actualizado
	 * @throws AddressNotFoundException Si la dirección no existe
	 */
	@Override
	public AddressDto update(final AddressDto addressDto) {
		log.info("Actualizando dirección con ID: {}", addressDto.getAddressId());

		Address existingAddress = this.addressRepository.findById(addressDto.getAddressId())
				.orElseThrow(() -> new AddressNotFoundException("Dirección no encontrada"));

		// Actualizar campos de dirección
		existingAddress.setFullAddress(addressDto.getFullAddress());
		existingAddress.setPostalCode(addressDto.getPostalCode());
		existingAddress.setCity(addressDto.getCity());

		Address updatedAddress = this.addressRepository.save(existingAddress);
		log.info("Dirección actualizada exitosamente");
		return AddressMappingHelper.map(updatedAddress);
	}

	/**
	 * Actualiza una dirección existente usando su identificador como parámetro.
	 * Actualiza los campos: calle completa, código postal y ciudad.
	 * 
	 * @param addressId Identificador de la dirección a actualizar
	 * @param addressDto Datos con las actualizaciones
	 * @return AddressDto actualizado
	 * @throws AddressNotFoundException Si la dirección no existe
	 */
	@Override
	public AddressDto update(final Integer addressId, final AddressDto addressDto) {
		log.info("Actualizando dirección con ID: {} desde parámetro", addressId);

		Address existingAddress = addressRepository.findById(addressId)
				.orElseThrow(() -> new AddressNotFoundException("Dirección no encontrada con ID: " + addressId));

		// Actualizar campos de dirección
		existingAddress.setFullAddress(addressDto.getFullAddress());
		existingAddress.setPostalCode(addressDto.getPostalCode());
		existingAddress.setCity(addressDto.getCity());

		Address updatedAddress = addressRepository.save(existingAddress);
		log.info("Dirección actualizada exitosamente con ID: {}", addressId);
		return AddressMappingHelper.map(updatedAddress);
	}

	// ============================================================================
	// ELIMINACIÓN - Métodos para eliminar direcciones
	// ============================================================================

	/**
	 * Elimina una dirección del sistema por su identificador.
	 * 
	 * @param addressId Identificador de la dirección a eliminar
	 */
	@Override
	public void deleteById(final Integer addressId) {
		log.info("Eliminando dirección con ID: {}", addressId);
		this.addressRepository.deleteById(addressId);
		log.info("Dirección eliminada exitosamente con ID: {}", addressId);
	}

}
