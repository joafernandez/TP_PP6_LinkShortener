package app.enlace;

/** Un intento de inserción encontró un alias ocupado; el service puede probar otro candidato. */
public class ColisionAliasException extends RuntimeException {

	public ColisionAliasException(Throwable causa) {
		super("El alias ya está ocupado.", causa);
	}
}
