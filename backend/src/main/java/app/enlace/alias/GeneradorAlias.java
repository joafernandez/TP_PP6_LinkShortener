package app.enlace.alias;

import java.util.Set;

/**
 * Estrategia de generación de alias (punto de extensión, ADR-0010 y ADR-0027).
 * No accede a la base ni garantiza unicidad: recibe los alias ocupados y la base resuelve las carreras.
 */
public interface GeneradorAlias {

	/** Devuelve un alias que no está entre los ocupados y nunca es una palabra reservada. */
	String generar(Set<String> ocupados);
}
