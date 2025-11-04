package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.exception.wrapper.PaymentServiceException;
import com.selimhorri.app.helper.PaymentMappingHelper;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de pagos.
 * Gestiona la lógica de negocio para operaciones CRUD de pagos,
 * incluyendo consultas, creación, actualización de estado y cancelación.
 * Integra con el servicio de órdenes para validar y actualizar estados.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final RestTemplate restTemplate;

	// ========== BÚSQUEDA ==========

	/**
	 * Obtiene todos los pagos asociados a órdenes en estado IN_PAYMENT.
	 * Enriquece cada pago con datos completos de la orden desde el servicio de órdenes.
	 *
	 * @return Lista de pagos con órdenes en estado IN_PAYMENT
	 */
	@Override
	public List<PaymentDto> findAll() {
		log.info("Obteniendo lista de pagos con órdenes en estado IN_PAYMENT");

		return this.paymentRepository.findAll()
				.stream()
				.map(PaymentMappingHelper::map)
				.filter(p -> {
					try {
						OrderDto orderDto = this.restTemplate.getForObject(
								AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/"
										+ p.getOrderDto().getOrderId(),
								OrderDto.class);

						// Verificar si la orden tiene estado IN_PAYMENT
						if (orderDto != null && "IN_PAYMENT".equalsIgnoreCase(orderDto.getOrderStatus())) {
							p.setOrderDto(orderDto);
							return true;
						}
						return false;

					} catch (Exception e) {
						log.error("Error enriqueciendo pago ID {} con datos de orden: {}", p.getPaymentId(), e.getMessage());
						return false;
					}
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Obtiene un pago específico por su ID.
	 * Enriquece el pago con datos completos de la orden desde el servicio de órdenes.
	 *
	 * @param paymentId ID del pago a buscar
	 * @return Pago encontrado enriquecido con datos de orden
	 * @throws PaymentServiceException Si el pago no existe o hay error en la integración
	 */
	@Override
	public PaymentDto findById(final Integer paymentId) {
		log.info("Buscando pago con ID: {}", paymentId);
		PaymentDto paymentDto = this.paymentRepository.findById(paymentId)
				.map(PaymentMappingHelper::map)
				.orElseThrow(
						() -> new PaymentServiceException(String.format("Pago con ID %d no encontrado", paymentId)));

		try {
			OrderDto orderDto = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/"
							+ paymentDto.getOrderDto().getOrderId(),
					OrderDto.class);
			paymentDto.setOrderDto(orderDto);
			return paymentDto;
		} catch (Exception e) {
			log.error("Error enriqueciendo pago {} con datos de orden: {}", paymentId, e.getMessage());
			throw new PaymentServiceException("No se pudo obtener información de la orden para el pago");
		}
	}

	// ========== CREACIÓN ==========

	/**
	 * Crea un nuevo pago validando que la orden asociada exista y esté en estado ORDERED.
	 * Después de guardar el pago, actualiza el estado de la orden a IN_PAYMENT.
	 *
	 * @param paymentDto Datos del nuevo pago con orden requerida
	 * @return Pago creado como DTO
	 * @throws IllegalArgumentException Si la orden no existe o no está en estado ORDERED
	 * @throws PaymentServiceException Si hay error en la integración con el servicio de órdenes
	 */
	@Override
	@Transactional
	public PaymentDto save(final PaymentDto paymentDto) {
		log.info("Guardando nuevo pago para orden ID: {}", paymentDto.getOrderDto().getOrderId());

		// Validar que el pago tenga orden asociada
		if (paymentDto.getOrderDto() == null || paymentDto.getOrderDto().getOrderId() == null) {
			throw new IllegalArgumentException("El ID de la orden debe estar presente");
		}

		try {
			// 1. Verificar existencia y estado de la orden
			OrderDto orderDto = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/"
							+ paymentDto.getOrderDto().getOrderId(),
					OrderDto.class);

			if (orderDto == null) {
				throw new PaymentServiceException(
						"Orden con ID " + paymentDto.getOrderDto().getOrderId() + " no encontrada");
			}
			if (!orderDto.getOrderStatus().equals(OrderStatus.ORDERED.name())) {
				throw new IllegalArgumentException(
						"No se puede procesar pago de una orden que no está en estado ORDERED");
			}

			// 2. Guardar el pago
			PaymentDto savedPayment = PaymentMappingHelper.map(
					this.paymentRepository.save(PaymentMappingHelper.mapForPayment(paymentDto)));

			// 3. Actualizar estado de la orden a IN_PAYMENT
			String patchUrl = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/"
					+ paymentDto.getOrderDto().getOrderId() + "/status";

			try {
				this.restTemplate.patchForObject(patchUrl, null, Void.class);
				log.info("Estado de orden {} actualizado a IN_PAYMENT", paymentDto.getOrderDto().getOrderId());
			} catch (RestClientException e) {
				log.error("Error al actualizar estado de orden {}: {}", paymentDto.getOrderDto().getOrderId(), e.getMessage());
				throw new PaymentServiceException("Pago guardado pero falló la actualización del estado de orden: " + e.getMessage());
			}

			return savedPayment;

		} catch (HttpClientErrorException.NotFound ex) {
			throw new PaymentServiceException("Orden con ID " + paymentDto.getOrderDto().getOrderId() + " no encontrada");
		} catch (RestClientException ex) {
			throw new PaymentServiceException("Error procesando pago: " + ex.getMessage());
		}
	}

	// ========== ACTUALIZACIÓN DE ESTADO ==========

	/**
	 * Actualiza el estado de un pago siguiendo la transición de estados:
	 * NOT_STARTED -> IN_PROGRESS -> COMPLETED (terminal)
	 *
	 * @param paymentId ID del pago a actualizar
	 * @return Pago con estado actualizado como DTO
	 * @throws PaymentNotFoundException Si el pago no existe
	 * @throws IllegalStateException Si el pago está en estado terminal (COMPLETED o CANCELED)
	 */
	@Override
	public PaymentDto updateStatus(final int paymentId) {
		log.info("Actualizando estado de pago con ID: {}", paymentId);

		return this.paymentRepository.findById(paymentId)
				.map(payment -> {
					PaymentStatus currentStatus = payment.getPaymentStatus();
					PaymentStatus newStatus;

					// Transición de estados
					switch (currentStatus) {
						case NOT_STARTED:
							newStatus = PaymentStatus.IN_PROGRESS;
							break;
						case IN_PROGRESS:
							newStatus = PaymentStatus.COMPLETED;
							break;
						case COMPLETED:
							throw new IllegalStateException(
									"Pago ya está COMPLETADO y no puede actualizarse más");
						case CANCELED:
							throw new IllegalStateException("Pago está CANCELADO y no puede actualizarse");
						default:
							throw new IllegalStateException("Estado de pago desconocido: " + currentStatus);
					}

					payment.setPaymentStatus(newStatus);
					log.info("Pago {} transicionó de {} a {}", paymentId, currentStatus, newStatus);

					return PaymentMappingHelper.map(this.paymentRepository.save(payment));
				})
				.orElseThrow(() -> new PaymentNotFoundException("Pago con ID " + paymentId + " no encontrado"));
	}

	// ========== ELIMINACIÓN ==========

	/**
	 * Cancela un pago existente estableciendo su estado a CANCELED.
	 * Solo permite cancelar pagos en estado NOT_STARTED o IN_PROGRESS.
	 * Los pagos COMPLETED o CANCELED no pueden ser modificados.
	 *
	 * @param paymentId ID del pago a cancelar
	 * @throws IllegalArgumentException Si el pago no existe o ya está completado/cancelado
	 */
	@Override
	@Transactional
	public void deleteById(final Integer paymentId) {
		log.info("Cancelando pago con ID: {}", paymentId);

		Payment payment = this.paymentRepository.findById(paymentId)
				.orElseThrow(() -> new IllegalArgumentException("Pago con ID " + paymentId + " no encontrado"));

		if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
			log.warn("Intento de cancelar pago {} en estado COMPLETED", paymentId);
			throw new IllegalArgumentException("No se puede cancelar un pago completado");
		}

		if (payment.getPaymentStatus() == PaymentStatus.CANCELED) {
			log.warn("Intento de cancelar pago {} que ya está CANCELED", paymentId);
			throw new IllegalArgumentException("El pago ya está cancelado");
		}

		payment.setPaymentStatus(PaymentStatus.CANCELED);
		this.paymentRepository.save(payment);
		log.info("Pago {} cancelado exitosamente", paymentId);
	}
}