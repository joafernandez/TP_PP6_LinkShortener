package app.link.alias;

/**
 * Estrategia de generación de alias (punto de extensión, ADR-0010).
 * No garantiza unicidad: eso lo resuelve el servicio contra la base de datos.
 */
public interface AliasGenerator {

	/** Genera un alias candidato que nunca es una palabra reservada. */
	String generate();
}
