package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.helper.CartMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de carritos.
 * Gestiona la lógica de negocio para operaciones CRUD de carritos,
 * incluyendo consultas, creación y eliminación (soft delete).
 * Integra con el servicio de usuarios para validar y enriquecer datos del carrito.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

	private final CartRepository cartRepository;
	@LoadBalanced
	private final RestTemplate restTemplate;

	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene todos los carritos activos de la base de datos.
	 * Enriquece cada carrito con datos del usuario desde el servicio de usuarios.
	 * Maneja errores de forma resiliente - devuelve carritos sin enriquecer si falla.
	 *
	 * @return Lista de carritos activos enriquecidos como DTOs
	 */
	@Override
	public List<CartDto> findAll() {
		log.info("Obteniendo lista completa de carritos activos desde la base de datos");
		return this.cartRepository.findAllByIsActiveTrue()
				.stream()
				.map(CartMappingHelper::map)
				.map(this::enrichCartWithUserData)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Obtiene un carrito activo específico por su ID.
	 * Enriquece el carrito con datos del usuario desde el servicio de usuarios.
	 * Lanza excepción si el carrito no existe o no está activo.
	 *
	 * @param cartId ID del carrito a buscar
	 * @return Carrito encontrado enriquecido como DTO
	 * @throws CartNotFoundException Si el carrito activo no existe
	 */
	@Override
	public CartDto findById(final Integer cartId) {
		log.info("Buscando carrito activo con ID: {}", cartId);
		return this.cartRepository.findByCartIdAndIsActiveTrue(cartId)
				.map(CartMappingHelper::map)
				.map(this::enrichCartWithUserData)
				.orElseThrow(() -> new CartNotFoundException(
						String.format("Carrito activo con ID %d no encontrado", cartId)));
	}

	// ========== CREACIÓN ==========

	/**
	 * Crea un nuevo carrito en la base de datos.
	 * Valida que el usuario exista consultando el servicio de usuarios.
	 * Mapea el DTO a entidad, persiste y convierte el resultado a DTO.
	 *
	 * @param cartDto Datos del nuevo carrito con userId requerido
	 * @return Carrito creado como DTO
	 * @throws IllegalArgumentException Si userId es nulo
	 * @throws UserNotFoundException Si el usuario no existe
	 * @throws RuntimeException Si hay error comunicándose con el servicio de usuarios
	 */
	@Override
	public CartDto save(final CartDto cartDto) {
		log.info("Guardando nuevo carrito para usuario ID: {}", cartDto.getUserId());

		if (cartDto.getUserId() == null) {
			throw new IllegalArgumentException("UserId debe estar presente al crear un carrito");
		}

		try {
			validateUserExists(cartDto.getUserId());
		} catch (HttpClientErrorException.NotFound ex) {
			throw new UserNotFoundException(String.format("Usuario con ID %d no encontrado", cartDto.getUserId()));
		} catch (RestClientException ex) {
			throw new RuntimeException("Error verificando existencia del usuario: " + ex.getMessage(), ex);
		}

		cartDto.setCartId(null);
		cartDto.setOrderDtos(null);
		return CartMappingHelper.map(this.cartRepository.save(CartMappingHelper.map(cartDto)));
	}

	// ========== ELIMINACIÓN ==========

	/**
	 * Elimina un carrito de forma lógica (soft delete) estableciendo isActive en false.
	 * El carrito permanece en la base de datos pero se marca como inactivo.
	 *
	 * @param cartId ID del carrito a eliminar
	 * @throws CartNotFoundException Si el carrito no existe
	 */
	@Override
	public void deleteById(final Integer cartId) {
		log.info("Eliminando carrito con ID: {} (soft delete)", cartId);

		Cart cart = this.cartRepository.findById(cartId)
				.orElseThrow(() -> new CartNotFoundException(
						String.format("Carrito con ID %d no encontrado", cartId)));

		cart.setActive(false);
		this.cartRepository.save(cart);

		log.debug("Carrito con ID {} marcado como inactivo", cartId);
	}

	// ========== MÉTODOS AUXILIARES ==========

	/**
	 * Enriquece un CartDto con datos del usuario desde el servicio de usuarios.
	 * Maneja errores de forma resiliente - devuelve el carrito sin enriquecer si falla.
	 *
	 * @param cart CartDto a enriquecer
	 * @return CartDto con datos de usuario completos o sin enriquecer si hay error
	 */
	private CartDto enrichCartWithUserData(CartDto cart) {
		try {
			UserDto userDto = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + cart.getUserDto().getUserId(),
					UserDto.class);
			if (userDto != null) {
				cart.setUserDto(userDto);
			}
			return cart;
		} catch (HttpClientErrorException.NotFound e) {
			log.warn("Usuario no encontrado para userId: {}", cart.getUserDto().getUserId());
			return cart;
		} catch (Exception e) {
			log.error("Error enriqueciendo carrito con datos de usuario para userId: {}", 
					cart.getUserDto().getUserId(), e);
			return null;
		}
	}

	/**
	 * Valida que un usuario exista consultando el servicio de usuarios.
	 *
	 * @param userId ID del usuario a validar
	 * @return UserDto si el usuario existe
	 * @throws HttpClientErrorException.NotFound Si el usuario no existe
	 * @throws RestClientException Si hay error en la comunicación
	 */
	private UserDto validateUserExists(Integer userId) {
		String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
		UserDto userDto = this.restTemplate.getForObject(url, UserDto.class);
		if (userDto == null) {
			throw new UserNotFoundException(String.format("Usuario con ID %d no encontrado", userId));
		}
		return userDto;
	}

}
