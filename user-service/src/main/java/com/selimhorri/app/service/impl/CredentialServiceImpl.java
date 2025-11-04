package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.exception.wrapper.CredentialNotFoundException;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.exception.wrapper.UsernameAlreadyExistsException;
import com.selimhorri.app.helper.CredentialMappingHelper;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.CredentialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de gestión de credenciales de usuario.
 * Proporciona operaciones CRUD para credenciales, incluyendo validación de usuario,
 * codificación de contraseñas y verificación de duplicados de nombre de usuario.
 * 
 * @author Sistema de Gestión de Usuarios
 * @version 1.0
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CredentialServiceImpl implements CredentialService {

	private final CredentialRepository credentialRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	// ============================================================================
	// BÚSQUEDA - Métodos para obtener credenciales existentes
	// ============================================================================

	/**
	 * Obtiene la lista completa de todas las credenciales del sistema.
	 * 
	 * @return Lista inmutable de todos los CredentialDto disponibles
	 */
	@Override
	public List<CredentialDto> findAll() {
		log.info("Obteniendo lista completa de credenciales");
		return this.credentialRepository.findAll()
				.stream()
				.map(CredentialMappingHelper::map)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Busca una credencial específica por su identificador.
	 * 
	 * @param credentialId Identificador único de la credencial
	 * @return CredentialDto correspondiente al ID proporcionado
	 * @throws CredentialNotFoundException Si la credencial no existe
	 */
	@Override
	public CredentialDto findById(final Integer credentialId) {
		log.info("Buscando credencial con ID: {}", credentialId);
		return this.credentialRepository.findById(credentialId)
				.map(CredentialMappingHelper::map)
				.orElseThrow(() -> new CredentialNotFoundException(
						String.format("Credencial con ID: %d no encontrada", credentialId)));
	}

	/**
	 * Busca una credencial por nombre de usuario.
	 * 
	 * @param username Nombre de usuario para la búsqueda
	 * @return CredentialDto asociada al nombre de usuario
	 * @throws UserObjectNotFoundException Si no existe credencial para ese usuario
	 */
	@Override
	public CredentialDto findByUsername(final String username) {
		log.info("Buscando credencial para usuario: {}", username);
		return CredentialMappingHelper.map(this.credentialRepository.findByUsername(username)
				.orElseThrow(() -> new UserObjectNotFoundException(
						String.format("Credencial para usuario: %s no encontrada", username))));
	}

	// ============================================================================
	// CREACIÓN - Métodos para crear nuevas credenciales
	// ============================================================================

	/**
	 * Crea una nueva credencial para un usuario.
	 * Valida que el nombre de usuario sea único y que el usuario exista.
	 * Codifica la contraseña antes de guardarla.
	 * 
	 * @param credentialDto Datos de la credencial a crear
	 * @return CredentialDto creada y persistida
	 * @throws UsernameAlreadyExistsException Si el nombre de usuario ya existe
	 * @throws UserObjectNotFoundException Si el usuario asociado no existe
	 * @throws IllegalArgumentException Si el usuario ya tiene credenciales
	 */
	@Override
	public CredentialDto save(final CredentialDto credentialDto) {
		log.info("Creando nueva credencial para usuario: {}", credentialDto.getUserDto().getUserId());
		credentialDto.setCredentialId(null);
		
		// Validar que el nombre de usuario es único
		if (credentialRepository.existsByUsername(credentialDto.getUsername())) {
			log.error("Intento de crear credencial con nombre de usuario duplicado: {}", credentialDto.getUsername());
			throw new UsernameAlreadyExistsException("Nombre de usuario ya existe: " + credentialDto.getUsername());
		}

		// Validar que el usuario existe
		Integer userId = credentialDto.getUserDto().getUserId();
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserObjectNotFoundException("Usuario no encontrado con ID: " + userId));

		// Validar que el usuario no tiene credenciales previas
		if (credentialRepository.existsByUserUserId(userId)) {
			log.error("Usuario con ID {} ya tiene credenciales asociadas", userId);
			throw new IllegalArgumentException(
					"Usuario con ID " + userId + " ya tiene credenciales. Intente actualizarlas en su lugar.");
		}

		// Codificar contraseña y guardar
		String rawPassword = credentialDto.getPassword();
		String encodedPassword = passwordEncoder.encode(rawPassword);
		credentialDto.setPassword(encodedPassword);

		Credential credential = CredentialMappingHelper.map(credentialDto);
		credential.setUser(user);

		Credential saved = credentialRepository.save(credential);
		log.info("Credencial creada exitosamente para usuario ID: {}", userId);
		return CredentialMappingHelper.map(saved);
	}

	// ============================================================================
	// ACTUALIZACIÓN - Métodos para actualizar credenciales existentes
	// ============================================================================

	/**
	 * Actualiza una credencial existente usando el objeto CredentialDto.
	 * Codifica la nueva contraseña y actualiza todos los campos de seguridad.
	 * 
	 * @param credentialDto Datos de la credencial con actualizaciones
	 * @return CredentialDto actualizado
	 * @throws CredentialNotFoundException Si la credencial no existe
	 */
	@Override
	public CredentialDto update(final CredentialDto credentialDto) {
		log.info("Actualizando credencial con ID: {}", credentialDto.getCredentialId());

		Credential existingCredential = credentialRepository.findById(credentialDto.getCredentialId())
				.orElseThrow(() -> new CredentialNotFoundException(
						"Credencial no encontrada con ID: " + credentialDto.getCredentialId()));

		// Actualizar campos de credencial
		existingCredential.setUsername(credentialDto.getUsername());
		String encodedPassword = passwordEncoder.encode(credentialDto.getPassword());
		existingCredential.setPassword(encodedPassword);

		// Actualizar campos de seguridad
		existingCredential.setRoleBasedAuthority(credentialDto.getRoleBasedAuthority());
		existingCredential.setIsEnabled(credentialDto.getIsEnabled());
		existingCredential.setIsAccountNonExpired(credentialDto.getIsAccountNonExpired());
		existingCredential.setIsAccountNonLocked(credentialDto.getIsAccountNonLocked());
		existingCredential.setIsCredentialsNonExpired(credentialDto.getIsCredentialsNonExpired());

		Credential updatedCredential = credentialRepository.save(existingCredential);
		log.info("Credencial actualizada exitosamente");
		return CredentialMappingHelper.map(updatedCredential);
	}

	/**
	 * Actualiza una credencial existente usando su identificador como parámetro.
	 * Codifica la nueva contraseña y actualiza todos los campos de seguridad.
	 * 
	 * @param credentialId Identificador de la credencial a actualizar
	 * @param credentialDto Datos con las actualizaciones
	 * @return CredentialDto actualizado
	 * @throws CredentialNotFoundException Si la credencial no existe
	 */
	@Override
	public CredentialDto update(final Integer credentialId, final CredentialDto credentialDto) {
		log.info("Actualizando credencial con ID: {} desde parámetro", credentialId);

		Credential existingCredential = credentialRepository.findById(credentialId)
				.orElseThrow(() -> new CredentialNotFoundException(
						"Credencial no encontrada con ID: " + credentialId));

		// Actualizar campos de credencial
		existingCredential.setUsername(credentialDto.getUsername());
		String encodedPassword = passwordEncoder.encode(credentialDto.getPassword());
		existingCredential.setPassword(encodedPassword);

		// Actualizar campos de seguridad
		existingCredential.setRoleBasedAuthority(credentialDto.getRoleBasedAuthority());
		existingCredential.setIsEnabled(credentialDto.getIsEnabled());
		existingCredential.setIsAccountNonExpired(credentialDto.getIsAccountNonExpired());
		existingCredential.setIsAccountNonLocked(credentialDto.getIsAccountNonLocked());
		existingCredential.setIsCredentialsNonExpired(credentialDto.getIsCredentialsNonExpired());

		Credential updatedCredential = this.credentialRepository.save(existingCredential);
		log.info("Credencial actualizada exitosamente con ID: {}", credentialId);
		return CredentialMappingHelper.map(updatedCredential);
	}

	// ============================================================================
	// ELIMINACIÓN - Métodos para eliminar credenciales
	// ============================================================================

	/**
	 * Elimina una credencial del sistema por su identificador.
	 * Verifica que la credencial existe antes de eliminarla.
	 * 
	 * @param credentialId Identificador de la credencial a eliminar
	 * @throws CredentialNotFoundException Si la credencial no existe
	 */
	@Transactional
	@Override
	public void deleteById(final Integer credentialId) {
		log.info("Eliminando credencial con ID: {}", credentialId);

		boolean exists = credentialRepository.existsById(credentialId);
		if (!exists) {
			log.error("Intento de eliminar credencial inexistente con ID: {}", credentialId);
			throw new CredentialNotFoundException("Credencial con ID: " + credentialId + " no encontrada");
		}

		this.credentialRepository.deleteByCredentialId(credentialId);
		log.info("Credencial eliminada exitosamente con ID: {}", credentialId);
	}
}