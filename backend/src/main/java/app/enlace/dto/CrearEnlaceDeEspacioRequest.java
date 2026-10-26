package app.enlace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido de acortamiento dentro del espacio ({@code POST /api/v1/me/links}). El código es opcional. */
@Schema(name = "CreateNamespaceLinkRequest")
public record CrearEnlaceDeEspacioRequest(
		@NotBlank(message = "La URL es obligatoria.")
		@Size(max = 2048, message = "La URL no puede superar los 2048 caracteres.")
		String url,
		@JsonProperty("code") String codigo) {
}
