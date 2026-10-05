package app.link;

/** La URL a acortar no cumple el criterio de validez (ADR-0020) o pertenece al propio servicio (ADR-0019). */
public class InvalidUrlException extends RuntimeException {

	public InvalidUrlException(String message) {
		super(message);
	}
}
