package app.espacio;

/** El nombre de espacio pedido ya pertenece a otro usuario. */
public class EspacioOcupadoException extends RuntimeException {

	public EspacioOcupadoException(String nombre, Throwable causa) {
		super("El espacio '" + nombre + "' ya está en uso. Elegí otro.", causa);
	}
}
