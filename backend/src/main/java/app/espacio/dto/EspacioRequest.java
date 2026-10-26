package app.espacio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/** Pedido para elegir o cambiar el espacio ({@code PUT /api/v1/me/namespace}). Lo valida {@code ValidadorEspacio}. */
@Schema(name = "NamespaceRequest")
public record EspacioRequest(@JsonProperty("namespace") String nombre) {
}
