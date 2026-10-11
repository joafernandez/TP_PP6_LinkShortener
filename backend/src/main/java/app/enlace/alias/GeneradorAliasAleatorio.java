package app.enlace.alias;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;

/**
 * Genera alias aleatorios con el alfabeto y el largo configurados (ADR-0016).
 * Usa {@link SecureRandom} para que los alias no puedan predecirse.
 */
@Component
public class GeneradorAliasAleatorio implements GeneradorAlias {

	private final AliasSettings configuracion;
	private final RandomGenerator aleatorio;

	@Autowired
	public GeneradorAliasAleatorio(AppProperties propiedades) {
		this(propiedades.alias(), new SecureRandom());
	}

	GeneradorAliasAleatorio(AliasSettings configuracion, RandomGenerator aleatorio) {
		this.configuracion = configuracion;
		this.aleatorio = aleatorio;
	}

	@Override
	public String generar() {
		String alias;
		do {
			alias = cadenaAleatoria();
		} while (configuracion.reservadas().contains(alias));
		return alias;
	}

	private String cadenaAleatoria() {
		String alfabeto = configuracion.alfabeto();
		StringBuilder resultado = new StringBuilder(configuracion.longitud());
		for (int i = 0; i < configuracion.longitud(); i++) {
			resultado.append(alfabeto.charAt(aleatorio.nextInt(alfabeto.length())));
		}
		return resultado.toString();
	}
}
