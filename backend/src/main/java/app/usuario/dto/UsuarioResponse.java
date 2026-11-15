package app.usuario.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.espacio.dto.NombreAnteriorResponse;
import app.usuario.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

/** Representación del usuario con sesión en la API. */
@Schema(name = "UserResponse")
public record UsuarioResponse(
		@JsonProperty("name") String nombre,
		String email,
		@JsonProperty("namespace") String espacio,
		@JsonProperty("previousNamespaces") List<NombreAnteriorResponse> anteriores) {

	public static UsuarioResponse desde(PerfilUsuario perfil) {
		return new UsuarioResponse(perfil.nombre(), perfil.email(), perfil.espacio(),
				NombreAnteriorResponse.desde(perfil.anteriores()));
	}
}
