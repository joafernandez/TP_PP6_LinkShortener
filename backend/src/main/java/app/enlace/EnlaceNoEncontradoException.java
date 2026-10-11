package app.enlace;

/** El alias no existe o su enlace está vencido. Ambos casos se tratan igual (ADR-0018). */
public class EnlaceNoEncontradoException extends RuntimeException {

	public EnlaceNoEncontradoException(String alias) {
		super("No hay un enlace vigente con el alias '" + alias + "'.");
	}
}
