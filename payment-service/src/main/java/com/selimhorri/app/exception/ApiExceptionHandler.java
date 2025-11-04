package com.selimhorri.app.exception;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.selimhorri.app.exception.payload.ExceptionMsg;
import com.selimhorri.app.exception.wrapper.PaymentServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador centralizado para manejar excepciones de toda la aplicación.
 * Proporciona respuestas consistentes de error con información contextual
 * y timestamps para todas las excepciones de validación, negocio y pagos.
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

	// ========== MANEJO DE EXCEPCIONES DE VALIDACIÓN ==========

	/**
	 * Maneja excepciones de validación de entrada.
	 * Captura errores de validación en payloads de solicitudes
	 * (campos requeridos, formatos incorrectos, etc.)
	 *
	 * @param e Excepción de validación que contiene detalles de los campos inválidos
	 * @return ResponseEntity con mensaje de error formateado y HTTP 400
	 */
	@ExceptionHandler(value = {
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class,
	})
	public <T extends BindException> ResponseEntity<ExceptionMsg> handleValidationException(final T e) {

		log.info("Petición rechazada: error en validación de campos de entrada");
		final var badRequest = HttpStatus.BAD_REQUEST;

		String errorMsg = "Error de validación en la solicitud";
		var fieldError = e.getBindingResult().getFieldError();
		if (fieldError != null) {
			errorMsg = fieldError.getDefaultMessage();
		}

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg(errorMsg)
						.httpStatus(badRequest)
						.timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
						.build(),
				badRequest);
	}

	// ========== MANEJO DE EXCEPCIONES DE NEGOCIO ==========

	/**
	 * Maneja excepciones de argumentos ilícitos y estado inválido.
	 * Captura errores cuando hay parámetros inválidos o transiciones de estado no permitidas
	 * en operaciones de pago y procesamiento.
	 *
	 * @param e Excepción de runtime que describe el error de negocio
	 * @return ResponseEntity con mensaje de error y HTTP 400
	 */
	@ExceptionHandler(value = {
			IllegalArgumentException.class,
			IllegalStateException.class
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleApiBadRequestException(final T e) {

		log.info("Conflicto detectado: argumento o estado inválido - {}", e.getMessage());
		final var badRequest = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg(e.getMessage())
						.httpStatus(badRequest)
						.timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
						.build(),
				badRequest);
	}

	// ========== MANEJO DE EXCEPCIONES DE PAGOS ==========

	/**
	 * Maneja excepciones específicas del servicio de pagos.
	 * Captura errores cuando ocurren problemas en el procesamiento de pagos,
	 * transacciones no encontradas o fallos en la integración con proveedores.
	 *
	 * @param e Excepción de pago que describe el error
	 * @return ResponseEntity con mensaje de error y HTTP 404
	 */
	@ExceptionHandler(value = {
			PaymentServiceException.class,
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleApiRequestException(final T e) {

		log.info("Error en servicio de pagos: {}", e.getMessage());
		final var notFound = HttpStatus.NOT_FOUND;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg(e.getMessage())
						.httpStatus(notFound)
						.timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
						.build(),
				notFound);
	}

}