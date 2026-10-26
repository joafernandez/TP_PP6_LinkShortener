package app.usuario;

/** El pedido necesita una sesión iniciada con Google y no la tiene. */
public class SesionRequeridaException extends RuntimeException {

	public SesionRequeridaException() {
		super("Iniciá sesión con Google para continuar.");
	}
}
