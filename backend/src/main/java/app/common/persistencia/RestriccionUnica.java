package app.common.persistencia;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Reconoce la violación de una restricción UNIQUE concreta en la cadena de causas de un error de persistencia.
 * Los Repository la usan para traducir solo esa colisión a una excepción del negocio (ADR-0024).
 */
public final class RestriccionUnica {

	private static final String SQLSTATE_UNICA = "23505";

	private RestriccionUnica() {
	}

	/** HSQLDB informa el nombre en mayúsculas y a veces con el esquema: {@code PUBLIC.UK_LINK_ALIAS}. */
	public static boolean esViolacion(Throwable error, String restriccion) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacion) {
				String nombre = violacion.getConstraintName();
				return SQLSTATE_UNICA.equals(violacion.getSQLState())
						&& (restriccion.equalsIgnoreCase(nombre) || ("PUBLIC." + restriccion).equalsIgnoreCase(nombre));
			}
		}
		return false;
	}
}
