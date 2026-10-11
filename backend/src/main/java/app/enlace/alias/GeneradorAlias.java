package app.enlace.alias;

/**
 * Estrategia de generación de alias (punto de extensión, ADR-0010).
 * No garantiza unicidad: eso lo resuelve el servicio contra la base de datos.
 */
public interface GeneradorAlias {

	/** Genera un alias candidato que nunca es una palabra reservada. */
	String generar();
}
