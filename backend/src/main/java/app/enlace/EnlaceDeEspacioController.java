package app.enlace;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.enlace.dto.CrearEnlaceDeEspacioRequest;
import app.enlace.dto.EnlaceResponse;
import app.usuario.SesionRequeridaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Enlaces dentro del espacio del usuario con sesión (contrato: docs/api/openapi.yaml, ADR-0032). */
@RestController
@RequestMapping("/api/v1/me/links")
@Tag(name = "Usuario", description = "Usuario con sesión iniciada con Google")
public class EnlaceDeEspacioController {

	private final EnlaceDeEspacioService enlaceService;

	public EnlaceDeEspacioController(EnlaceDeEspacioService enlaceService) {
		this.enlaceService = enlaceService;
	}

	@PostMapping
	@Operation(operationId = "createNamespaceLink", summary = "Acortar una URL dentro del espacio del usuario")
	public ResponseEntity<EnlaceResponse> crear(@AuthenticationPrincipal OidcUser usuarioGoogle,
			@Valid @RequestBody CrearEnlaceDeEspacioRequest solicitud) {
		if (usuarioGoogle == null) {
			throw new SesionRequeridaException();
		}
		EnlaceDeEspacio enlace = enlaceService.crear(usuarioGoogle.getSubject(), solicitud.url(), solicitud.codigo());
		String urlCorta = enlaceService.urlCortaDe(enlace);
		return ResponseEntity.created(URI.create(urlCorta)).body(new EnlaceResponse(enlace.getAlias(), urlCorta,
				enlace.getUrlOriginal(), enlace.getCreadoEn(), enlace.getVenceEn()));
	}
}
