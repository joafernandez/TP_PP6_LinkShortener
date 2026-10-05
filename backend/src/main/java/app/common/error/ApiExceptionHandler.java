package app.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import app.link.AliasUnavailableException;
import app.link.InvalidUrlException;

/**
 * Traduce las excepciones de la API a respuestas ProblemDetail (RFC 9457, ADR-0007).
 * Las excepciones estándar de Spring MVC (validación, JSON mal formado, etc.) las resuelve la clase base.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(InvalidUrlException.class)
	public ProblemDetail handleInvalidUrl(InvalidUrlException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(AliasUnavailableException.class)
	public ProblemDetail handleAliasUnavailable(AliasUnavailableException e) {
		log.warn(e.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"No se pudo generar un enlace corto en este momento. Intente nuevamente.");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception e) {
		log.error("Error inesperado", e);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");
	}
}
