package app.enlace;

/** No se encontró un alias libre dentro del máximo de intentos configurado. */
public class AliasNoDisponibleException extends RuntimeException {

	public AliasNoDisponibleException(int intentos) {
		super("No se pudo generar un alias libre después de " + intentos + " intentos.");
	}
}
