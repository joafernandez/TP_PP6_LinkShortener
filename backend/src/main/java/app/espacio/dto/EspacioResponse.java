package app.espacio.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/** Espacio guardado, en minúsculas, y sus nombres anteriores todavía en transición. */
@Schema(name = "NamespaceResponse")
public record EspacioResponse(
		@JsonProperty("namespace") String nombre,
		@JsonProperty("previousNamespaces") List<NombreAnteriorResponse> anteriores) {
}
