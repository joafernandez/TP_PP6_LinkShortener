package app.enlace.alias;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;

/**
 * Devuelve el menor código libre en orden shortlex: primero por largo y después por el orden del alfabeto
 * (ADR-0026). Con el alfabeto {@code 123...z} la secuencia es {@code 1, 2, ..., z, 11, 12, ...}.
 */
@Component
public class GeneradorCodigoSecuencial implements GeneradorAlias {

	private final AliasSettings configuracion;

	@Autowired
	public GeneradorCodigoSecuencial(AppProperties propiedades) {
		this(propiedades.alias());
	}

	GeneradorCodigoSecuencial(AliasSettings configuracion) {
		this.configuracion = configuracion;
	}

	/** Termina siempre: los ocupados y las reservadas son finitos y cada largo agrega más candidatos. */
	@Override
	public String generar(Set<String> ocupados) {
		String alfabeto = configuracion.alfabeto();
		for (int largo = 1;; largo++) {
			int[] posiciones = new int[largo];
			do {
				String candidato = armar(alfabeto, posiciones);
				if (!ocupados.contains(candidato) && !configuracion.reservadas().contains(candidato)) {
					return candidato;
				}
			} while (avanzar(posiciones, alfabeto.length()));
		}
	}

	private static String armar(String alfabeto, int[] posiciones) {
		StringBuilder codigo = new StringBuilder(posiciones.length);
		for (int posicion : posiciones) {
			codigo.append(alfabeto.charAt(posicion));
		}
		return codigo.toString();
	}

	/** Suma uno como en un odómetro. Devuelve false cuando se recorrieron todos los códigos de ese largo. */
	private static boolean avanzar(int[] posiciones, int base) {
		for (int i = posiciones.length - 1; i >= 0; i--) {
			if (++posiciones[i] < base) {
				return true;
			}
			posiciones[i] = 0;
		}
		return false;
	}
}
