package app.enlace.alias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import app.common.config.AppProperties.AliasSettings;

/** El menor código libre en orden shortlex (ADR-0026), sin base de datos. */
class GeneradorCodigoSecuencialTest {

	private static final String ALFABETO = "123456789abcdefghijkmnpqrstuvwxyz";

	@Test
	void sinCodigosOcupadosDevuelveElPrimeroDelAlfabeto() {
		assertThat(generador(ALFABETO, Set.of()).generar(Set.of())).isEqualTo("1");
	}

	@Test
	void asignaEnElOrdenDelAlfabeto() {
		assertThat(generador(ALFABETO, Set.of()).generar(Set.of("1", "2"))).isEqualTo("3");
		assertThat(generador(ALFABETO, Set.of()).generar(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9")))
				.isEqualTo("a");
	}

	@Test
	void devuelveElMenorLibreAunqueHayaOcupadosDespues() {
		// el "2" venció: se reutiliza antes de seguir con el "4"
		assertThat(generador(ALFABETO, Set.of()).generar(Set.of("1", "3"))).isEqualTo("2");
	}

	@Test
	void recorreLaSecuenciaCompletaSinRepetir() {
		GeneradorCodigoSecuencial generador = generador(ALFABETO, Set.of());
		Set<String> ocupados = new HashSet<>();

		for (int i = 0; i < ALFABETO.length(); i++) {
			String codigo = generador.generar(ocupados);
			assertThat(codigo).hasSize(1).isEqualTo(String.valueOf(ALFABETO.charAt(i)));
			ocupados.add(codigo);
		}

		assertThat(generador.generar(ocupados)).isEqualTo("11");
	}

	@Test
	void alAgotarseLosDeUnCaracterSigueConLosDeDosEnOrden() {
		GeneradorCodigoSecuencial generador = generador("ab", Set.of());

		assertThat(generador.generar(Set.of("a", "b"))).isEqualTo("aa");
		assertThat(generador.generar(Set.of("a", "b", "aa"))).isEqualTo("ab");
		assertThat(generador.generar(Set.of("a", "b", "aa", "ab"))).isEqualTo("ba");
		assertThat(generador.generar(Set.of("a", "b", "aa", "ab", "ba", "bb"))).isEqualTo("aaa");
	}

	@Test
	void saltaLasPalabrasReservadas() {
		GeneradorCodigoSecuencial generador = generador("ab", Set.of("a", "ab"));

		assertThat(generador.generar(Set.of())).isEqualTo("b");
		assertThat(generador.generar(Set.of("b", "aa"))).isEqualTo("ba");
	}

	@Test
	void ignoraOcupadosQueNoPertenecenALaSecuencia() {
		// enlaces de la Etapa 1, con alias aleatorios de 5 caracteres, conviven con la secuencia nueva
		assertThat(generador(ALFABETO, Set.of()).generar(Set.of("xT3se", "AB12C"))).isEqualTo("1");
	}

	private static GeneradorCodigoSecuencial generador(String alfabeto, Set<String> reservadas) {
		return new GeneradorCodigoSecuencial(new AliasSettings(alfabeto, 10, reservadas));
	}
}
