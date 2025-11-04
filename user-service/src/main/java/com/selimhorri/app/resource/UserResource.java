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

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller para la gestión de usuarios del sistema.
 * Proporciona endpoints para obtener, crear, actualizar y eliminar usuarios.
 * Expone los datos en formato JSON en la ruta base /api/users.
 * 
 * @author Sistema de Gestión de Usuarios
 * @version 1.0
 */
@RestController
@RequestMapping(value = {"/api/users"})
@Slf4j
@RequiredArgsConstructor
public class UserResource {
	
	private final UserService userService;

	// ============================================================================
	// BÚSQUEDA - Endpoints para obtener usuarios
	// ============================================================================

	/**
	 * Obtiene la lista completa de todos los usuarios del sistema.
	 * 
	 * @return ResponseEntity con lista de todos los usuarios con credenciales
	 */
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<UserDto>> findAll() {
		log.info("Obteniendo lista completa de usuarios");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.userService.findAll()));
	}
	
	/**
	 * Obtiene un usuario específico por su identificador.
	 * 
	 * @param userId Identificador del usuario (como parámetro de ruta)
	 * @return ResponseEntity con el usuario solicitado
	 * @throws UserObjectNotFoundException Si el usuario no existe
	 */
	@GetMapping("/{userId}")
	public ResponseEntity<UserDto> findById(
			@PathVariable("userId") 
			@NotBlank(message = "El ID de usuario no debe estar vacío") 
			@Valid final String userId) {
		log.info("Buscando usuario con ID: {}", userId);
		return ResponseEntity.ok(this.userService.findById(Integer.parseInt(userId.strip())));
	}
	
	/**
	 * Obtiene un usuario por su nombre de usuario.
	 * 
	 * @param username Nombre de usuario para la búsqueda
	 * @return ResponseEntity con el usuario solicitado
	 * @throws UserObjectNotFoundException Si no existe usuario con ese nombre
	 */
	@GetMapping("/username/{username}")
	public ResponseEntity<UserDto> findByUsername(
			@PathVariable("username") 
			@NotBlank(message = "El nombre de usuario no debe estar vacío") 
			@Valid final String username) {
		log.info("Buscando usuario con nombre de usuario: {}", username);
		return ResponseEntity.ok(this.userService.findByUsername(username));
	}

	// ============================================================================
	// CREACIÓN - Endpoints para crear nuevos usuarios
	// ============================================================================

	/**
	 * Crea un nuevo usuario en el sistema.
	 * 
	 * @param userDto Datos del usuario a crear (en el cuerpo de la solicitud)
	 * @return ResponseEntity con el usuario creado
	 */
	@PostMapping
	public ResponseEntity<UserDto> save(
			@RequestBody 
			@NotNull(message = "Los datos de usuario no deben ser nulos") 
			@Valid final UserDto userDto) {
		log.info("Creando nuevo usuario: {} {}", userDto.getFirstName(), userDto.getLastName());
		return ResponseEntity.ok(this.userService.save(userDto));
	}

	// ============================================================================
	// ACTUALIZACIÓN - Endpoints para actualizar usuarios existentes
	// ============================================================================

	/**
	 * Actualiza un usuario existente usando el objeto UserDto.
	 * 
	 * @param userDto Datos del usuario con actualizaciones (en el cuerpo de la solicitud)
	 * @return ResponseEntity con el usuario actualizado
	 * @throws EntityNotFoundException Si el usuario no existe
	 */
	@PutMapping
	public ResponseEntity<UserDto> update(
			@RequestBody 
			@NotNull(message = "Los datos de usuario no deben ser nulos") 
			@Valid final UserDto userDto) {
		log.info("Actualizando usuario con ID: {}", userDto.getUserId());
		return ResponseEntity.ok(this.userService.update(userDto));
	}
	
	/**
	 * Actualiza un usuario existente usando su identificador como parámetro de ruta.
	 * 
	 * @param userId Identificador del usuario (como parámetro de ruta)
	 * @param userDto Datos con las actualizaciones (en el cuerpo de la solicitud)
	 * @return ResponseEntity con el usuario actualizado
	 * @throws EntityNotFoundException Si el usuario no existe
	 */
	@PutMapping("/{userId}")
	public ResponseEntity<UserDto> update(
			@PathVariable("userId") 
			@NotBlank(message = "El ID de usuario no debe estar vacío") final String userId, 
			@RequestBody 
			@NotNull(message = "Los datos de usuario no deben ser nulos") 
			@Valid final UserDto userDto) {
		log.info("Actualizando usuario con ID desde parámetro: {}", userId);
		return ResponseEntity.ok(this.userService.update(Integer.parseInt(userId.strip()), userDto));
	}

	// ============================================================================
	// ELIMINACIÓN - Endpoints para eliminar usuarios
	// ============================================================================

	/**
	 * Elimina un usuario del sistema por su identificador.
	 * Esta operación también elimina las credenciales asociadas.
	 * 
	 * @param userId Identificador del usuario (como parámetro de ruta)
	 * @return ResponseEntity indicando éxito de la operación
	 */
	@DeleteMapping("/{userId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("userId") 
			@NotBlank(message = "El ID de usuario no debe estar vacío") 
			@Valid final String userId) {
		log.info("Eliminando usuario con ID: {}", userId);
		this.userService.deleteById(Integer.parseInt(userId));
		log.info("Usuario eliminado exitosamente");
		return ResponseEntity.ok(true);
	}	
	
}