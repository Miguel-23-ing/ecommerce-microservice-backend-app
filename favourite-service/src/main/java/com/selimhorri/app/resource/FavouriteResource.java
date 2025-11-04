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

import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.FavouriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Controlador REST para operaciones CRUD de favoritos
@RestController
@RequestMapping("/api/favourites")
@Slf4j
@RequiredArgsConstructor
public class FavouriteResource {
	
	private final FavouriteService favouriteService;
	
	// Obtiene todos los favoritos registrados
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<FavouriteDto>> findAll() {
		log.info("Obteniendo lista completa de favoritos");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.favouriteService.findAll()));
	}
	
	// Obtiene un favorito específico por ID de usuario e ID de producto
	@GetMapping("/{userId}/{productId}")
	public ResponseEntity<FavouriteDto> findById(
			@PathVariable("userId") final String userId, 
			@PathVariable("productId") final String productId) {
		log.info("Buscando favorito para usuario {} y producto {}", userId, productId);
		return ResponseEntity.ok(this.favouriteService.findById(
				new FavouriteId(Integer.parseInt(userId), Integer.parseInt(productId), null )));
	}
	
	// Crea un nuevo favorito validando que el DTO no sea nulo y sea válido
	@PostMapping
	public ResponseEntity<FavouriteDto> save(
			@RequestBody 
			@NotNull(message = "Input must not be NULL") 
			@Valid final FavouriteDto favouriteDto) {
		log.info("Creando nuevo favorito para usuario {} y producto {}", 
				favouriteDto.getUserId(), favouriteDto.getProductId());
		return ResponseEntity.ok(this.favouriteService.save(favouriteDto));
	}
	
	
	// Elimina un favorito específico por ID de usuario e ID de producto
	@DeleteMapping("/{userId}/{productId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("userId") final String userId, 
			@PathVariable("productId") final String productId) {
		log.info("Eliminando favorito para usuario {} y producto {}", userId, productId);
		this.favouriteService.deleteById(new FavouriteId(Integer.parseInt(userId), Integer.parseInt(productId), null));
		return ResponseEntity.ok(true);
	}	
	
}