package app.enlace;

/** La URL a acortar no cumple el criterio de validez (ADR-0020) o pertenece al propio servicio (ADR-0019). */
public class UrlInvalidaException extends RuntimeException {

	public UrlInvalidaException(String message) {
		super(message);
	}
}
