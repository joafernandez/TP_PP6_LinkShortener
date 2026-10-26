package app.usuario;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Usuario con sesión (contrato: docs/api/openapi.yaml, ADR-0030). */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Usuario", description = "Usuario con sesión iniciada con Google")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	/** Sin sesión, Spring entrega un principal nulo y se responde 401. */
	@GetMapping
	@Operation(operationId = "getCurrentUser", summary = "Usuario con sesión iniciada")
	public UsuarioResponse actual(@AuthenticationPrincipal OidcUser usuarioGoogle) {
		if (usuarioGoogle == null) {
			throw new SesionRequeridaException();
		}
		return UsuarioResponse.desde(usuarioService.perfil(usuarioGoogle.getSubject()));
	}
}
