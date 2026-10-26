package app.espacio;

/** El nombre de espacio pedido no cumple las reglas (ADR-0028 y ADR-0029); el mensaje explica cuál. */
public class NombreEspacioInvalidoException extends RuntimeException {

	public NombreEspacioInvalidoException(String mensaje) {
		super(mensaje);
	}
}
