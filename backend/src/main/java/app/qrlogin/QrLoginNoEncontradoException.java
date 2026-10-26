package app.qrlogin;

/** El pedido de login con QR no existe, o el secreto no corresponde (no se distinguen). */
public class QrLoginNoEncontradoException extends RuntimeException {

	public QrLoginNoEncontradoException() {
		super("El pedido de inicio de sesión no existe o ya no está disponible.");
	}
}
