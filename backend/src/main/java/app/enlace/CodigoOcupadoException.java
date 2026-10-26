package app.enlace;

/** El código personalizado pertenece a un enlace vigente del mismo espacio. */
public class CodigoOcupadoException extends RuntimeException {

	public CodigoOcupadoException(String codigo) {
		super("El código '" + codigo + "' ya está en uso en tu espacio. Elegí otro.");
	}
}
