package app.qrlogin;

/** El pedido existe pero no admite la operación: venció, ya fue respondido, no fue autorizado o ya se usó. */
public class QrLoginNoVigenteException extends RuntimeException {

	public QrLoginNoVigenteException() {
		super("El pedido de inicio de sesión venció, ya fue respondido o no fue autorizado. Generá un código nuevo.");
	}
}
