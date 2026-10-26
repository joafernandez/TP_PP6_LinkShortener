package app.espacio;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import app.common.config.AppProperties;

/**
 * Reglas del nombre de espacio: minúsculas y números (requerimiento del Cliente), hasta 15 caracteres
 * y sin nombres reservados (decisiones técnicas del equipo, ADR-0029).
 */
@Component
public class ValidadorEspacio {

	private static final Pattern PERMITIDOS = Pattern.compile("[a-z0-9]+");

	private final Set<String> reservados;

	public ValidadorEspacio(AppProperties propiedades) {
		this.reservados = propiedades.espacio().reservados();
	}

	/**
	 * Devuelve el nombre en minúsculas si es válido.
	 *
	 * @throws NombreEspacioInvalidoException con un mensaje que explica la regla incumplida
	 */
	public String normalizar(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new NombreEspacioInvalidoException("Escribí el nombre de tu espacio.");
		}
		String normalizado = Espacio.normalizar(nombre);
		if (!PERMITIDOS.matcher(normalizado).matches()) {
			throw new NombreEspacioInvalidoException(
					"El espacio solo puede tener letras sin acentos y números, sin espacios ni símbolos.");
		}
		if (normalizado.length() > Espacio.LARGO_MAXIMO) {
			throw new NombreEspacioInvalidoException(
					"El espacio puede tener hasta " + Espacio.LARGO_MAXIMO + " caracteres.");
		}
		if (reservados.contains(normalizado)) {
			throw new NombreEspacioInvalidoException("Ese nombre está reservado por el sistema. Elegí otro.");
		}
		return normalizado;
	}
}
