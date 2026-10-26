package app.enlace;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;

/**
 * URLs cortas públicas: {@code GET /{alias}} (anónima) y {@code GET /{espacio}/{código}} (de un usuario, ADR-0032).
 * <p>
 * Los segmentos no admiten puntos, para no capturar los recursos estáticos ni Swagger, y las rutas literales
 * como {@code /v3/api-docs} tienen prioridad sobre los patrones (ADR-0011).
 * Redirige con 302 y no con 301, porque el alias vence y puede reasignarse (ADR-0012).
 */
@Controller
public class RedireccionController {

	private static final Resource PAGINA_NO_ENCONTRADO = new ClassPathResource("pages/link-not-found.html");

	private final EnlaceService enlaceService;
	private final EnlaceDeEspacioService enlaceDeEspacioService;

	public RedireccionController(EnlaceService enlaceService, EnlaceDeEspacioService enlaceDeEspacioService) {
		this.enlaceService = enlaceService;
		this.enlaceDeEspacioService = enlaceDeEspacioService;
	}

	@GetMapping("/{espacio:[A-Za-z0-9]+}/{codigo:[A-Za-z0-9_-]+}")
	@Operation(operationId = "redirectNamespaceLink")
	public ResponseEntity<Void> redirigirEnEspacio(@PathVariable String espacio, @PathVariable String codigo) {
		EnlaceDeEspacio enlace = enlaceDeEspacioService.resolver(espacio, codigo);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(enlace.getUrlOriginal()))
				.build();
	}

	@GetMapping("/{alias:[A-Za-z0-9]+}")
	@Operation(operationId = "redirect")
	public ResponseEntity<Void> redirigir(@PathVariable String alias) {
		Enlace enlace = enlaceService.resolver(alias);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(enlace.getUrlOriginal()))
				.build();
	}

	/**
	 * Quien abre la URL corta es una persona en un navegador: se responde con una página HTML (ADR-0018).
	 * Este manejador local tiene prioridad sobre el ProblemDetail en JSON de la API.
	 */
	@ExceptionHandler(EnlaceNoEncontradoException.class)
	public ResponseEntity<Resource> manejarNoEncontrado() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
				.body(PAGINA_NO_ENCONTRADO);
	}
}
