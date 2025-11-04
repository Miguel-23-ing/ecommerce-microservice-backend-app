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
import com.selimhorri.app.exception.wrapper.DuplicateEntityException;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Controlador centralizado para manejar excepciones de toda la aplicación
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

	// Maneja excepciones de validación de entrada (campos inválidos o formato incorrecto)
	@ExceptionHandler(value = {
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class,
	})
	public <T extends BindException> ResponseEntity<ExceptionMsg> handleValidationException(final T e) {

		log.info("Petición rechazada: error en validación de campos de entrada");
		final var badRequest = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg("*" + e.getBindingResult().getFieldError().getDefaultMessage() + "!**")
						.httpStatus(badRequest)
						.timestamp(ZonedDateTime
								.now(ZoneId.systemDefault()))
						.build(),
				badRequest);
	}

	// Maneja excepciones cuando la entidad ya existe en la base de datos (conflicto)
	@ExceptionHandler(value = {
			DuplicateEntityException.class
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleApiBadRequestException(final T e) {

		log.info("Conflicto detectado: entidad duplicada - {}", e.getMessage());
		final var badRequest = HttpStatus.CONFLICT;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg("#### " + e.getMessage() + "! ####")
						.httpStatus(badRequest)
						.timestamp(ZonedDateTime
								.now(ZoneId.systemDefault()))
						.build(),
				badRequest);
	}

	// Maneja excepciones cuando no se encuentra la entidad solicitada (404)
	@ExceptionHandler(value = {
			FavouriteNotFoundException.class,
			ProductNotFoundException.class,
			UserNotFoundException.class
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleApiRequestException(final T e) {

		log.info("Recurso no encontrado: {}", e.getMessage());
		final var badRequest = HttpStatus.NOT_FOUND;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.msg("#### " + e.getMessage() + "! ####")
						.httpStatus(badRequest)
						.timestamp(ZonedDateTime
								.now(ZoneId.systemDefault()))
						.build(),
				badRequest);
	}

}
