package com.selimhorri.app.exception;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.selimhorri.app.exception.payload.ExceptionMsg;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manejador centralizado de excepciones para la API de envíos.
 * Captura excepciones específicas y devuelve respuestas HTTP estructuradas
 * con mensajes de error consistentes y códigos de estado apropiados.
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

	// ========== VALIDACIÓN ==========

	/**
	 * Maneja excepciones de validación de entrada.
	 * Captura errores en la validación de parámetros de métodos y deserialización JSON.
	 *
	 * @param e Excepción de validación (MethodArgumentNotValidException o HttpMessageNotReadableException)
	 * @return ResponseEntity con código 400 (Bad Request) y mensaje de error
	 */
	@ExceptionHandler(value = {
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class,
	})
	public <T extends BindException> ResponseEntity<ExceptionMsg> handleValidationException(final T e) {
		log.info("Petición rechazada por validación inválida: {}", 
			e.getBindingResult().getFieldError() != null ? 
			e.getBindingResult().getFieldError().getDefaultMessage() : 
			"Error de deserialización");

		final var badRequest = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg(e.getBindingResult().getFieldError() != null ?
							e.getBindingResult().getFieldError().getDefaultMessage() :
							"Formato de solicitud inválido")
						.httpStatus(badRequest)
						.timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
						.build(),
				badRequest);
	}

	// ========== NEGOCIO ==========

	/**
	 * Maneja excepciones de lógica de negocio.
	 * Captura errores de estado ilegal y argumentos inválidos en operaciones de envío.
	 *
	 * @param e Excepción de negocio (IllegalStateException o IllegalArgumentException)
	 * @return ResponseEntity con código 400 (Bad Request) y mensaje de error
	 */
	@ExceptionHandler(value = {
			IllegalStateException.class,
			IllegalArgumentException.class
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleBusinessException(final T e) {
		log.info("Error en operación de envío: {}", e.getMessage());

		final var badRequest = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg(e.getMessage())
						.httpStatus(badRequest)
						.timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
						.build(),
				badRequest);
	}

	// ========== ENVÍOS ==========

	/**
	 * Maneja excepciones cuando no se encuentra un recurso de envío.
	 * Captura errores de artículos de pedido no encontrados y operaciones vacías en base de datos.
	 *
	 * @param e Excepción de recurso no encontrado (OrderItemNotFoundException o EmptyResultDataAccessException)
	 * @return ResponseEntity con código 404 (Not Found) y mensaje de error
	 */
	@ExceptionHandler(value = {
			OrderItemNotFoundException.class,
			EmptyResultDataAccessException.class
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleShippingNotFoundException(final T e) {
		log.info("Recurso de envío no encontrado: {}", e.getMessage());

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