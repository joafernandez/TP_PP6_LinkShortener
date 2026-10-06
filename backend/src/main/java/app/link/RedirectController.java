package app.link;

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

/**
 * URL corta pública: {@code GET /{alias}}.
 * <p>
 * Solo acepta un segmento alfanumérico, para no capturar los recursos estáticos, Swagger ni la API (ADR-0011).
 * Redirige con 302 y no con 301, porque el alias vence y puede reasignarse (ADR-0012).
 */
@Controller
public class RedirectController {

	private static final Resource NOT_FOUND_PAGE = new ClassPathResource("pages/link-not-found.html");

	private final LinkService linkService;

	public RedirectController(LinkService linkService) {
		this.linkService = linkService;
	}

	@GetMapping("/{alias:[A-Za-z0-9]+}")
	public ResponseEntity<Void> redirect(@PathVariable String alias) {
		Link link = linkService.resolve(alias);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(link.getOriginalUrl()))
				.build();
	}

	/**
	 * Quien abre la URL corta es una persona en un navegador: se responde con una página HTML (ADR-0018).
	 * Este manejador local tiene prioridad sobre el ProblemDetail en JSON de la API.
	 */
	@ExceptionHandler(LinkNotFoundException.class)
	public ResponseEntity<Resource> handleNotFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
				.body(NOT_FOUND_PAGE);
	}
}
