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

import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.CredentialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para operaciones CRUD de credenciales de usuario.
 * Expone endpoints para consultar, crear, actualizar y eliminar credenciales
 * de usuarios registrados en el sistema.
 */
@RestController
@RequestMapping(value = { "/api/credentials" })
@Slf4j
@RequiredArgsConstructor
public class CredentialResource {

	private final CredentialService credentialService;

	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene la lista completa de credenciales del sistema.
	 *
	 * @return ResponseEntity con lista de CredentialDto envueltos en DtoCollectionResponse
	 */
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<CredentialDto>> findAll() {
		log.info("Obteniendo lista completa de credenciales");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.credentialService.findAll()));
	}

	/**
	 * Obtiene una credencial específica por su ID.
	 *
	 * @param credentialId ID de la credencial a buscar (no puede ser en blanco)
	 * @return ResponseEntity con CredentialDto encontrado
	 */
	@GetMapping("/{credentialId}")
	public ResponseEntity<CredentialDto> findById(
			@PathVariable("credentialId") @NotBlank(message = "El ID de credencial no puede estar en blanco") @Valid final String credentialId) {
		log.info("Buscando credencial con ID: {}", credentialId);
		return ResponseEntity.ok(this.credentialService.findById(Integer.parseInt(credentialId.strip())));
	}

	/**
	 * Obtiene una credencial específica por nombre de usuario.
	 *
	 * @param username Nombre de usuario a buscar (no puede ser en blanco)
	 * @return ResponseEntity con CredentialDto encontrado
	 */
	@GetMapping("/username/{username}")
	public ResponseEntity<CredentialDto> findByUsername(
			@PathVariable("username") @NotBlank(message = "El nombre de usuario no puede estar en blanco") @Valid final String username) {
		log.info("Buscando credencial para usuario: {}", username);
		return ResponseEntity.ok(this.credentialService.findByUsername(username));
	}

	// ========== CREACIÓN ==========

	/**
	 * Crea una nueva credencial de usuario.
	 *
	 * @param credentialDto Datos de la credencial a crear (no puede ser nulo)
	 * @return ResponseEntity con CredentialDto creado
	 */
	@PostMapping
	public ResponseEntity<CredentialDto> save(
			@RequestBody @NotNull(message = "El objeto de entrada no puede ser nulo") @Valid final CredentialDto credentialDto) {
		log.info("Creando nueva credencial para usuario: {}", credentialDto.getUsername());
		return ResponseEntity.ok(this.credentialService.save(credentialDto));
	}

	// ========== ACTUALIZACIÓN ==========

	/**
	 * Actualiza una credencial existente (sin especificar ID en ruta).
	 *
	 * @param credentialDto Datos de la credencial a actualizar (no puede ser nulo)
	 * @return ResponseEntity con CredentialDto actualizado
	 */
	@PutMapping
	public ResponseEntity<CredentialDto> update(
			@RequestBody @NotNull(message = "El objeto de entrada no puede ser nulo") @Valid final CredentialDto credentialDto) {
		log.info("Actualizando credencial para usuario: {}", credentialDto.getUsername());
		return ResponseEntity.ok(this.credentialService.update(credentialDto));
	}

	/**
	 * Actualiza una credencial específica por su ID.
	 *
	 * @param credentialId ID de la credencial a actualizar (no puede ser en blanco)
	 * @param credentialDto Datos de la credencial a actualizar (no puede ser nulo)
	 * @return ResponseEntity con CredentialDto actualizado
	 */
	@PutMapping("/{credentialId}")
	public ResponseEntity<CredentialDto> update(
			@PathVariable("credentialId") @NotBlank(message = "El ID de credencial no puede estar en blanco") final String credentialId,
			@RequestBody @NotNull(message = "El objeto de entrada no puede ser nulo") @Valid final CredentialDto credentialDto) {
		log.info("Actualizando credencial con ID: {}", credentialId);
		return ResponseEntity.ok(this.credentialService.update(Integer.parseInt(credentialId.strip()), credentialDto));
	}

	// ========== ELIMINACIÓN ==========

	/**
	 * Elimina una credencial por su ID.
	 *
	 * @param credentialId ID de la credencial a eliminar (no puede ser en blanco)
	 * @return ResponseEntity con true indicando éxito de la operación
	 */
	@DeleteMapping("/{credentialId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("credentialId") @NotBlank(message = "El ID de credencial no puede estar en blanco") @Valid final String credentialId) {
		log.info("Eliminando credencial con ID: {}", credentialId);
		this.credentialService.deleteById(Integer.parseInt(credentialId));
		return ResponseEntity.ok(true);
	}

}