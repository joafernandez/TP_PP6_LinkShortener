package app.enlace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/** Pedido de acortamiento ({@code POST /api/v1/links}). */
@Schema(name = "CreateLinkRequest")
public record CrearEnlaceRequest(
		@NotBlank(message = "La URL es obligatoria.")
		@Size(max = 2048, message = "La URL no puede superar los 2048 caracteres.")
		String url) {
}
