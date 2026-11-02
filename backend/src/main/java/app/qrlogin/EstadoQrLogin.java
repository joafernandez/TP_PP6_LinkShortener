package app.qrlogin;

/**
 * Estado de un pedido de login con QR (ADR-0033).
 * {@code EXPIRED} nunca se guarda: se calcula al consultar, cuando un pedido pendiente o autorizado ya venció.
 */
public enum EstadoQrLogin {
	PENDING,
	AUTHORIZED,
	REJECTED,
	USED,
	EXPIRED
}
