package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de gestión de usuarios.
 * Proporciona operaciones CRUD para usuarios, incluyendo validación de credenciales,
 * filtrado de usuarios con credenciales y sincronización de datos de usuario.
 * 
 * @author Sistema de Gestión de Usuarios
 * @version 1.0
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final CredentialRepository credentialRepository;
	private final PasswordEncoder passwordEncoder;

	// ============================================================================
	// BÚSQUEDA - Métodos para obtener usuarios existentes
	// ============================================================================

	/**
	 * Obtiene la lista completa de todos los usuarios que tienen credenciales.
	 * Filtra usuarios sin credenciales para garantizar consistencia en la autenticación.
	 * 
	 * @return Lista inmutable de todos los UserDto con credenciales disponibles
	 */
	@Override
	public List<UserDto> findAll() {
		log.info("Obteniendo lista completa de usuarios con credenciales");
		return this.userRepository.findAll()
				.stream()
				.filter(user -> user.getCredential() != null)
				.map(UserMappingHelper::map)
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Busca un usuario específico por su identificador.
	 * Valida que el usuario tenga credenciales antes de retornarlo.
	 * 
	 * @param userId Identificador único del usuario
	 * @return UserDto correspondiente al ID proporcionado
	 * @throws UserObjectNotFoundException Si el usuario no existe o no tiene credenciales
	 */
	@Override
	public UserDto findById(final Integer userId) {
		log.info("Buscando usuario con ID: {}", userId);
		return this.userRepository.findById(userId)
				.filter(user -> user.getCredential() != null)
				.map(UserMappingHelper::map)
				.orElseThrow(() -> new UserObjectNotFoundException(
						String.format("Usuario con ID: %d no encontrado o sin credenciales", userId)));
	}

	/**
	 * Busca un usuario por su nombre de usuario asociado en las credenciales.
	 * 
	 * @param username Nombre de usuario para la búsqueda
	 * @return UserDto asociado al nombre de usuario
	 * @throws UserObjectNotFoundException Si no existe usuario con ese nombre de usuario
	 */
	@Override
	public UserDto findByUsername(final String username) {
		log.info("Buscando usuario con nombre de usuario: {}", username);
		return UserMappingHelper.map(this.userRepository.findByCredentialUsername(username)
				.orElseThrow(() -> new UserObjectNotFoundException(
						String.format("Usuario con nombre de usuario: %s no encontrado", username))));
	}

	// ============================================================================
	// CREACIÓN - Métodos para crear nuevos usuarios
	// ============================================================================

	/**
	 * Crea un nuevo usuario en el sistema.
	 * Si el userDto incluye credenciales, las crea automáticamente y las asocia al usuario.
	 * 
	 * @param userDto Datos del usuario a crear (puede incluir credenciales)
	 * @return UserDto creado y persistido con credenciales si fueron proporcionadas
	 */
	@Override
	public UserDto save(final UserDto userDto) {
		log.info("Creando nuevo usuario");
		userDto.setUserId(null);
		
		// 1. Guardar primero el usuario sin credenciales
		User savedUser = this.userRepository.save(UserMappingHelper.mapOnlyUser(userDto));
		log.info("Usuario creado exitosamente con ID: {}", savedUser.getUserId());
		
		// 2. Crear credenciales automáticamente si no existen en el DTO
		String username;
		String password;
		RoleBasedAuthority role;
		
		if (userDto.getCredentialDto() != null && userDto.getCredentialDto().getUsername() != null) {
			// Usar credenciales del DTO
			log.info("Creando credenciales desde DTO para el usuario: {}", savedUser.getUserId());
			username = userDto.getCredentialDto().getUsername();
			password = userDto.getCredentialDto().getPassword();
			role = userDto.getCredentialDto().getRoleBasedAuthority();
		} else {
			// Auto-generar credenciales
			log.info("Auto-generando credenciales para el usuario: {}", savedUser.getUserId());
			username = savedUser.getFirstName().toLowerCase() + "." + savedUser.getLastName().toLowerCase();
			password = "password123";  // Password por defecto
			role = RoleBasedAuthority.ROLE_USER;
		}
		
		// Validar que el username no existe
		if (credentialRepository.existsByUsername(username)) {
			log.error("El nombre de usuario ya existe: {}", username);
			throw new IllegalArgumentException("El nombre de usuario ya existe: " + username);
		}
		
		// Crear las credenciales
		Credential credential = new Credential();
		credential.setUsername(username);
		credential.setPassword(passwordEncoder.encode(password));  // Encriptar con BCrypt
		credential.setRoleBasedAuthority(role);
		credential.setIsEnabled(true);
		credential.setIsAccountNonExpired(true);
		credential.setIsAccountNonLocked(true);
		credential.setIsCredentialsNonExpired(true);
		credential.setUser(savedUser);
		
		// Guardar las credenciales
		Credential savedCredential = credentialRepository.save(credential);
		savedUser.setCredential(savedCredential);
		log.info("✅ Credenciales creadas exitosamente: username={}, userId={}", username, savedUser.getUserId());
		
		return UserMappingHelper.map(savedUser);
	}

	// ============================================================================
	// ACTUALIZACIÓN - Métodos para actualizar usuarios existentes
	// ============================================================================

	/**
	 * Actualiza un usuario existente usando el objeto UserDto.
	 * Solo actualiza campos de perfil de usuario, no las credenciales.
	 * 
	 * @param userDto Datos del usuario con actualizaciones
	 * @return UserDto actualizado
	 * @throws EntityNotFoundException Si el usuario no existe o no tiene credenciales
	 */
	@Override
	public UserDto update(final UserDto userDto) {
		log.info("Actualizando usuario con ID: {}", userDto.getUserId());

		User existingUser = this.userRepository.findById(userDto.getUserId())
				.filter(user -> user.getCredential() != null)
				.orElseThrow(() -> new EntityNotFoundException(
						"Usuario no encontrado o sin credenciales (no se puede actualizar)"));

		// Actualizar campos de perfil permitidos
		existingUser.setFirstName(userDto.getFirstName());
		existingUser.setLastName(userDto.getLastName());
		existingUser.setImageUrl(userDto.getImageUrl());
		existingUser.setEmail(userDto.getEmail());
		existingUser.setPhone(userDto.getPhone());

		User updatedUser = this.userRepository.save(existingUser);
		log.info("Usuario actualizado exitosamente");
		return UserMappingHelper.map(updatedUser);
	}

	/**
	 * Actualiza un usuario existente usando su identificador como parámetro.
	 * Solo actualiza campos de perfil de usuario, no las credenciales.
	 * 
	 * @param userId Identificador del usuario a actualizar
	 * @param userDto Datos con las actualizaciones
	 * @return UserDto actualizado
	 * @throws EntityNotFoundException Si el usuario no existe o no tiene credenciales
	 */
	@Override
	public UserDto update(final Integer userId, final UserDto userDto) {
		log.info("Actualizando usuario con ID: {} desde parámetro", userId);

		User existingUser = this.userRepository.findById(userId)
				.filter(user -> user.getCredential() != null)
				.orElseThrow(() -> new EntityNotFoundException(
						"Usuario no encontrado con ID: " + userId + " o sin credenciales (no se puede actualizar)"));

		// Actualizar campos de perfil permitidos
		existingUser.setFirstName(userDto.getFirstName());
		existingUser.setLastName(userDto.getLastName());
		existingUser.setImageUrl(userDto.getImageUrl());
		existingUser.setEmail(userDto.getEmail());
		existingUser.setPhone(userDto.getPhone());

		User updatedUser = this.userRepository.save(existingUser);
		log.info("Usuario actualizado exitosamente con ID: {}", userId);
		return UserMappingHelper.map(updatedUser);
	}

	// ============================================================================
	// ELIMINACIÓN - Métodos para eliminar usuarios
	// ============================================================================

	/**
	 * Elimina un usuario del sistema por su identificador.
	 * Primero desvincula sus credenciales y luego las elimina de la base de datos.
	 * Finalmente elimina el registro del usuario.
	 * 
	 * @param userId Identificador del usuario a eliminar
	 * @throws EntityNotFoundException Si el usuario no existe
	 * @throws UserObjectNotFoundException Si el usuario no tiene credenciales para eliminar
	 */
	@Transactional
	@Override
	public void deleteById(final Integer userId) {
		log.info("Eliminando usuario con ID: {}", userId);

		// 1. Buscar el usuario y verificar que existe
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));

		// 2. Validar que tiene credenciales asociadas
		if (user.getCredential() == null) {
			log.error("Usuario con ID {} no tiene credenciales para eliminar", userId);
			throw new UserObjectNotFoundException("Usuario con ID: " + userId + " no tiene credenciales para eliminar");
		}

		// 3. Obtener el ID de las credenciales para borrarlas
		Integer credentialsId = user.getCredential().getCredentialId();
		log.info("Desviculando credenciales con ID: {} del usuario: {}", credentialsId, userId);

		// 4. Desvincular las credenciales del usuario (para evitar inconsistencias)
		user.setCredential(null);
		userRepository.save(user);

		// 5. Borrar las credenciales de la base de datos
		credentialRepository.deleteByCredentialId(credentialsId);
		log.info("Usuario eliminado exitosamente con ID: {}", userId);
	}
}