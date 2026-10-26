package app.espacio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/** Espacio guardado, en minúsculas. */
@Schema(name = "NamespaceResponse")
public record EspacioResponse(@JsonProperty("namespace") String nombre) {
}
