package app.enlace;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Reglas del código personalizado: minúsculas, números, {@code -} y {@code _} (requerimiento del Cliente)
 * y hasta 15 caracteres (límite técnico del equipo, ADR-0029). Se trata en minúsculas, como el espacio.
 */
@Component
public class ValidadorCodigo {

	private static final Pattern PERMITIDOS = Pattern.compile("[a-z0-9_-]+");

	/**
	 * Devuelve el código en minúsculas si es válido.
	 *
	 * @throws CodigoInvalidoException con un mensaje que explica la regla incumplida
	 */
	public String normalizar(String codigo) {
		String normalizado = codigo.toLowerCase(Locale.ROOT);
		if (!PERMITIDOS.matcher(normalizado).matches()) {
			throw new CodigoInvalidoException(
					"El código solo puede tener letras sin acentos, números, guion (-) y guion bajo (_), sin espacios.");
		}
		if (normalizado.length() > EnlaceDeEspacio.LARGO_MAXIMO_CODIGO) {
			throw new CodigoInvalidoException(
					"El código puede tener hasta " + EnlaceDeEspacio.LARGO_MAXIMO_CODIGO + " caracteres.");
		}
		return normalizado;
	}
}
