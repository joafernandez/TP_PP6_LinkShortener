package app.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido de acortamiento ({@code POST /api/v1/links}). */
public record CreateLinkRequest(
		@NotBlank(message = "La URL es obligatoria.")
		@Size(max = 2048, message = "La URL no puede superar los 2048 caracteres.")
		String url) {
}
