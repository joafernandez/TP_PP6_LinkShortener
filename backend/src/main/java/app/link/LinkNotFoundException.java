package app.link;

/** El alias no existe o su enlace está vencido. Ambos casos se tratan igual (ADR-0018). */
public class LinkNotFoundException extends RuntimeException {

	public LinkNotFoundException(String alias) {
		super("No hay un enlace vigente con el alias '" + alias + "'.");
	}
}
