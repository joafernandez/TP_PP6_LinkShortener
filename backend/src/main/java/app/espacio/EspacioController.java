package app.espacio;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.espacio.dto.EspacioRequest;
import app.espacio.dto.EspacioResponse;
import app.usuario.SesionRequeridaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Espacio de nombres del usuario con sesión (contrato: docs/api/openapi.yaml, ADR-0031). */
@RestController
@RequestMapping("/api/v1/me/namespace")
@Tag(name = "Usuario", description = "Usuario con sesión iniciada con Google")
public class EspacioController {

	private final EspacioService espacioService;

	public EspacioController(EspacioService espacioService) {
		this.espacioService = espacioService;
	}

	@PutMapping
	@Operation(operationId = "setNamespace", summary = "Elegir o cambiar el espacio de nombres")
	public EspacioResponse elegir(@AuthenticationPrincipal OidcUser usuarioGoogle,
			@RequestBody EspacioRequest solicitud) {
		if (usuarioGoogle == null) {
			throw new SesionRequeridaException();
		}
		Espacio espacio = espacioService.elegir(usuarioGoogle.getSubject(), solicitud.nombre());
		return new EspacioResponse(espacio.getNombre());
	}
}
