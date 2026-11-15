package app.espacio.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.espacio.NombreAnterior;
import io.swagger.v3.oas.annotations.media.Schema;

/** Un nombre anterior del espacio que sigue funcionando hasta {@code expiresAt} (ADR-0034). */
@Schema(name = "PreviousNamespace")
public record NombreAnteriorResponse(
		@JsonProperty("namespace") String nombre,
		@JsonProperty("expiresAt") Instant venceEn) {

	public static List<NombreAnteriorResponse> desde(List<NombreAnterior> anteriores) {
		return anteriores.stream().map(anterior -> new NombreAnteriorResponse(anterior.nombre(), anterior.venceEn())).toList();
	}
}
