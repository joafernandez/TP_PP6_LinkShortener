package app.enlace;

/** El código personalizado no cumple las reglas (ADR-0029 y ADR-0032); el mensaje explica cuál. */
public class CodigoInvalidoException extends RuntimeException {

	public CodigoInvalidoException(String mensaje) {
		super(mensaje);
	}
}
