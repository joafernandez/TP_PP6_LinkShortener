package app.enlace;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.enlace.dto.CrearEnlaceRequest;
import app.enlace.dto.EnlaceResponse;
import io.swagger.v3.oas.annotations.Operation;

/** API de enlaces (contrato: docs/api/openapi.yaml). */
@RestController
@RequestMapping("/api/v1/links")
public class EnlaceController {

	private final EnlaceService enlaceService;

	public EnlaceController(EnlaceService enlaceService) {
		this.enlaceService = enlaceService;
	}

	@PostMapping
	@Operation(operationId = "createLink")
	public ResponseEntity<EnlaceResponse> crear(@Valid @RequestBody CrearEnlaceRequest solicitud) {
		Enlace enlace = enlaceService.crear(solicitud.url());
		String urlCorta = enlaceService.urlCortaDe(enlace);
		return ResponseEntity.created(URI.create(urlCorta)).body(EnlaceResponse.desde(enlace, urlCorta));
	}
}
