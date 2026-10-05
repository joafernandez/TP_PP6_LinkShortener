package app.link;

/** No se encontró un alias libre dentro del máximo de intentos configurado. */
public class AliasUnavailableException extends RuntimeException {

	public AliasUnavailableException(int attempts) {
		super("No se pudo generar un alias libre después de " + attempts + " intentos.");
	}
}
