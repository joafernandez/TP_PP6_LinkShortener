package app.usuario;

/** Lo que se muestra del usuario con sesión. {@code espacio} es null si todavía no lo eligió. */
public record PerfilUsuario(String nombre, String email, String espacio) {
}
