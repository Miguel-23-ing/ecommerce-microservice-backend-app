package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para operaciones CRUD de pagos.
 * Proporciona endpoints para consultar, crear, actualizar estado y eliminar pagos.
 * Valida automáticamente los payloads de entrada mediante anotaciones JSR-303.
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentResource {

	private final PaymentService paymentService;

	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene la lista completa de pagos.
	 *
	 * @return ResponseEntity con lista de todos los pagos
	 */
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<PaymentDto>> findAll() {
		log.info("Obteniendo lista completa de pagos");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.paymentService.findAll()));
	}

	/**
	 * Obtiene un pago específico por su ID.
	 *
	 * @param paymentId ID del pago a buscar (no puede estar vacío)
	 * @return ResponseEntity con los datos del pago
	 */
	@GetMapping("/{paymentId}")
	public ResponseEntity<PaymentDto> findById(
			@PathVariable("paymentId") @NotBlank(message = "El ID del pago no puede estar vacío") @Valid final String paymentId) {
		log.info("Buscando pago con ID: {}", paymentId);
		return ResponseEntity.ok(this.paymentService.findById(Integer.parseInt(paymentId)));
	}

	// ========== CREACIÓN ==========

	/**
	 * Crea un nuevo pago.
	 *
	 * @param paymentDto Datos del nuevo pago a crear (requiere orden asociada)
	 * @return ResponseEntity con los datos del pago creado
	 */
	@PostMapping
	public ResponseEntity<PaymentDto> save(
			@RequestBody @NotNull(message = "El payload del pago no puede ser nulo") @Valid final PaymentDto paymentDto) {
		log.info("Creando nuevo pago para orden ID: {}", paymentDto.getOrderDto().getOrderId());
		return ResponseEntity.ok(this.paymentService.save(paymentDto));
	}

	// ========== ACTUALIZACIÓN DE ESTADO ==========

	/**
	 * Actualiza el estado de un pago mediante PATCH.
	 * Transición: NOT_STARTED -> IN_PROGRESS -> COMPLETED
	 *
	 * @param paymentId ID del pago cuyo estado será actualizado
	 * @return ResponseEntity con el pago con estado actualizado
	 */
	@PatchMapping("/{paymentId}")
	public ResponseEntity<PaymentDto> updateStatus(
			@PathVariable("paymentId") @NotBlank(message = "El ID del pago no puede estar vacío") @Valid final String paymentId) {
		log.info("Actualizando estado de pago con ID: {}", paymentId);
		return ResponseEntity.ok(this.paymentService.updateStatus(Integer.parseInt(paymentId)));
	}

	/**
	 * Actualiza el estado de un pago mediante PUT (alias de PATCH).
	 * Transición: NOT_STARTED -> IN_PROGRESS -> COMPLETED
	 *
	 * @param paymentId ID del pago cuyo estado será actualizado
	 * @return ResponseEntity con el pago con estado actualizado
	 */
	@PutMapping("/{paymentId}")
	public ResponseEntity<PaymentDto> updateStatusPut(
			@PathVariable("paymentId") @NotBlank(message = "El ID del pago no puede estar vacío") @Valid final String paymentId) {
		log.info("Actualizando estado de pago con ID: {} (PUT)", paymentId);
		return ResponseEntity.ok(this.paymentService.updateStatus(Integer.parseInt(paymentId)));
	}

	// ========== ELIMINACIÓN ==========

	/**
	 * Elimina (cancela) un pago existente por su ID.
	 * Solo permite cancelar pagos en estado NOT_STARTED o IN_PROGRESS.
	 *
	 * @param paymentId ID del pago a eliminar
	 * @return ResponseEntity con estado de éxito
	 */
	@DeleteMapping("/{paymentId}")
	public ResponseEntity<Boolean> deleteById(@PathVariable("paymentId") final String paymentId) {
		log.info("Eliminando (cancelando) pago con ID: {}", paymentId);
		this.paymentService.deleteById(Integer.parseInt(paymentId));
		return ResponseEntity.ok(true);
	}

}