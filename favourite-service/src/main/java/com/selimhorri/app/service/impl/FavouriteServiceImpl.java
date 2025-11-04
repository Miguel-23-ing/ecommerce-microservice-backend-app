package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.DuplicateEntityException;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.helper.FavouriteMappingHelper;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Implementación del servicio de favoritos con lógica de negocio
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {

	private final FavouriteRepository favouriteRepository;
	private final RestTemplate restTemplate;

	// ============================================================================
	// BÚSQUEDA - Métodos para obtener favoritos existentes
	// ============================================================================

	@Override
	public FavouriteDto findById(final FavouriteId favouriteId) {
		log.info("Buscando favorito: usuario={}, producto={}", favouriteId.getUserId(), favouriteId.getProductId());
		
		FavouriteDto favouriteDto = this.favouriteRepository
				.findByUserIdAndProductId(favouriteId.getUserId(), favouriteId.getProductId())
				.map(FavouriteMappingHelper::map)
				.orElseThrow(() -> new FavouriteNotFoundException(
						String.format("Favourite with userId: [%s] and productId: [%s] not found in database!",
								favouriteId.getUserId(),
								favouriteId.getProductId())));

		// Enriquecer con datos del usuario
		UserDto userDto = fetchUserData(favouriteDto.getUserId());
		favouriteDto.setUserDto(userDto);

		// Enriquecer con datos del producto
		ProductDto productDto = fetchProductData(favouriteDto.getProductId());
		favouriteDto.setProductDto(productDto);

		return favouriteDto;
	}

	@Override
	public List<FavouriteDto> findAll() {
		log.info("Obteniendo lista completa de favoritos");
		
		return this.favouriteRepository.findAll()
				.stream()
				.map(FavouriteMappingHelper::map)
				.map(this::enrichFavouriteWithExternalData)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	// ============================================================================
	// CREACIÓN - Método para guardar favoritos
	// ============================================================================

	@Override
	public FavouriteDto save(final FavouriteDto favouriteDto) {
		log.info("Guardando nuevo favorito: usuario={}, producto={}", favouriteDto.getUserId(), favouriteDto.getProductId());
		
		// Validar que el usuario existe
		validateUserExists(favouriteDto.getUserId());

		// Validar que el producto existe y no hay duplicados
		validateProductExistsAndNoDuplicates(favouriteDto.getUserId(), favouriteDto.getProductId());
	
		return FavouriteMappingHelper.map(
				this.favouriteRepository.save(FavouriteMappingHelper.map(favouriteDto)));
	}

	// ============================================================================
	// ELIMINACIÓN - Método para eliminar favoritos
	// ============================================================================

	@Override
	@Transactional
	public void deleteById(FavouriteId favouriteId) {
		log.info("Eliminando favorito: usuario={}, producto={}", favouriteId.getUserId(), favouriteId.getProductId());
		
		// Validar que existe antes de eliminar
		if (!favouriteRepository.existsByUserIdAndProductId(favouriteId.getUserId(), favouriteId.getProductId())) {
			throw new FavouriteNotFoundException(
					String.format("Favourite not found with userId: %s and productId: %s",
							favouriteId.getUserId(),
							favouriteId.getProductId()));
		}

		// Eliminar favorito
		favouriteRepository.deleteByUserIdAndProductId(favouriteId.getUserId(), favouriteId.getProductId());
	}

	// ============================================================================
	// MÉTODOS AUXILIARES - Lógica de validación y enriquecimiento de datos
	// ============================================================================

	// Obtiene datos del usuario desde user-service
	private UserDto fetchUserData(Integer userId) {
		try {
			UserDto userDto = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId,
					UserDto.class);
			
			if (userDto == null) {
				throw new UserNotFoundException(String.format("User with id: [%s] not found!", userId));
			}
			return userDto;
		} catch (RestClientException e) {
			throw new FavouriteNotFoundException(String.format("Error fetching user with id: [%s]", userId), e);
		}
	}

	// Obtiene datos del producto desde product-service
	private ProductDto fetchProductData(Integer productId) {
		try {
			ProductDto productDto = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId,
					ProductDto.class);
			
			if (productDto == null) {
				throw new ProductNotFoundException(String.format("Product with id: [%s] not found!", productId));
			}
			return productDto;
		} catch (RestClientException e) {
			throw new FavouriteNotFoundException(String.format("Error fetching product with id: [%s]", productId), e);
		}
	}

	// Enriquece un favorito con datos externos (usuario y producto)
	private FavouriteDto enrichFavouriteWithExternalData(FavouriteDto f) {
		try {
			UserDto userDto = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + f.getUserId(),
					UserDto.class);
			ProductDto productDto = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + f.getProductId(),
					ProductDto.class);

			if (userDto == null || productDto == null) {
				log.warn("Usuario {} o producto {} no encontrado, excluyendo favorito", f.getUserId(), f.getProductId());
				return null;
			}

			f.setUserDto(userDto);
			f.setProductDto(productDto);
			return f;
		} catch (Exception e) {
			log.warn("Error enriqueciendo favorito (usuario: {}, producto: {}): {}", 
					f.getUserId(), f.getProductId(), e.getMessage());
			return null;
		}
	}

	// Valida que el usuario existe
	private void validateUserExists(Integer userId) {
		try {
			ResponseEntity<UserDto> response = this.restTemplate.getForEntity(
					AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId,
					UserDto.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new UserNotFoundException(String.format("Usuario [%s] no encontrado", userId));
			}
		} catch (RestClientException e) {
			throw new UserNotFoundException(String.format("Error verificando usuario [%s]", userId), e);
		}
	}

	// Valida que el producto existe y no hay duplicados
	private void validateProductExistsAndNoDuplicates(Integer userId, Integer productId) {
		try {
			ResponseEntity<ProductDto> response = this.restTemplate.getForEntity(
					AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId,
					ProductDto.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new ProductNotFoundException(String.format("Producto [%s] no encontrado", productId));
			}

			boolean favouriteExists = this.favouriteRepository.existsByUserIdAndProductId(userId, productId);
			if (favouriteExists) {
				throw new DuplicateEntityException(
						String.format("El favorito ya existe para usuario [%s] y producto [%s]", userId, productId));
			}

		} catch (RestClientException e) {
			throw new ProductNotFoundException(String.format("Error verificando producto [%s]", productId), e);
		}
	}

}
