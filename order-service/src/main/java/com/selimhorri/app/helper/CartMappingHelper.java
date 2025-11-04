package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;

/**
 * Utilidad para mapeo entre entidades de Carrito y DTOs.
 * Proporciona métodos estáticos para convertir entre:
 * - Cart (entidad JPA) <-> CartDto (DTO de transferencia)
 * 
 * Maneja la conversión de atributos e inicialización de usuario.
 */
public interface CartMappingHelper {
	
	// ========== MAPEOS BÁSICOS ==========
	
	/**
	 * Convierte una entidad Cart a CartDto.
	 * Inicializa el UserDto con el userId asociado al carrito
	 * para posterior enriquecimiento con datos del servicio de usuarios.
	 *
	 * @param cart Entidad Cart a convertir
	 * @return CartDto con datos del carrito y usuario inicializado
	 */
	public static CartDto map(final Cart cart) {
		return CartDto.builder()
				.cartId(cart.getCartId())
				.userId(cart.getUserId())
				.userDto(
						UserDto.builder()
							.userId(cart.getUserId())
							.build())
				.build();
	}
	
	/**
	 * Convierte un CartDto a entidad Cart.
	 * Mapea los datos básicos del carrito al modelo de persistencia.
	 *
	 * @param cartDto DTO de carrito a convertir
	 * @return Entidad Cart con los datos del DTO
	 */
	public static Cart map(final CartDto cartDto) {
		return Cart.builder()
				.cartId(cartDto.getCartId())
				.userId(cartDto.getUserId())
				.build();
	}
}










