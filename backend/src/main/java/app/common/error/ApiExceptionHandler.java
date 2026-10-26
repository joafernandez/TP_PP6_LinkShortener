package app.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import app.enlace.AliasNoDisponibleException;
import app.enlace.UrlInvalidaException;
import app.enlace.EnlaceNoEncontradoException;
import app.usuario.SesionRequeridaException;

/**
 * Traduce las excepciones de la API a respuestas ProblemDetail (RFC 9457, ADR-0007).
 * Las excepciones estándar de Spring MVC (validación, JSON mal formado, etc.) las resuelve la clase base.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(UrlInvalidaException.class)
	public ProblemDetail manejarUrlInvalida(UrlInvalidaException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(AliasNoDisponibleException.class)
	public ProblemDetail manejarAliasNoDisponible(AliasNoDisponibleException e) {
		log.warn(e.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"No se pudo generar un enlace corto en este momento. Intente nuevamente.");
	}

	@ExceptionHandler(EnlaceNoEncontradoException.class)
	public ResponseEntity<ProblemDetail> manejarEnlaceNoEncontrado() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"El enlace no existe o está vencido.");
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.cacheControl(CacheControl.noStore())
				.body(problem);
	}

	@ExceptionHandler(SesionRequeridaException.class)
	public ProblemDetail manejarSesionRequerida(SesionRequeridaException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception e) {
		log.error("Error inesperado", e);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");
	}
}
