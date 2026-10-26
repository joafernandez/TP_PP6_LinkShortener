package app.enlace;

/** El usuario con sesión todavía no eligió su espacio, y lo necesita para acortar (criterio E3, minuta 02). */
public class EspacioNoElegidoException extends RuntimeException {

	public EspacioNoElegidoException() {
		super("Elegí tu espacio antes de crear enlaces.");
	}
}
