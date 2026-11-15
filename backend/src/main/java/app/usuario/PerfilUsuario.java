package app.usuario;

import java.util.List;

import app.espacio.NombreAnterior;

/**
 * Lo que se muestra del usuario con sesión. {@code espacio} es null si todavía no lo eligió; {@code anteriores}
 * son sus nombres anteriores que siguen en transición (ADR-0034).
 */
public record PerfilUsuario(String nombre, String email, String espacio, List<NombreAnterior> anteriores) {
}
