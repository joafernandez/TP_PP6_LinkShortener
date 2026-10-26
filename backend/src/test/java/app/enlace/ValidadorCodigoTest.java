package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorCodigoTest {

	private final ValidadorCodigo validador = new ValidadorCodigo();

	@ParameterizedTest
	@ValueSource(strings = { "final", "final-2026", "tp_java", "1", "a-b_c", "abcdefghijklmno" })
	void aceptaMinusculasNumerosGuionYGuionBajoHasta15Caracteres(String codigo) {
		assertThat(validador.normalizar(codigo)).isEqualTo(codigo);
	}

	@Test
	void trataLasMayusculasComoMinusculas() {
		assertThat(validador.normalizar("Final-2026")).isEqualTo("final-2026");
	}

	@ParameterizedTest
	@ValueSource(strings = { "mi codigo", "final!", "tp.java", "a/b", "año", "50%", "?x" })
	void rechazaEspaciosYCaracteresQueRompenLaUrl(String codigo) {
		assertThatThrownBy(() -> validador.normalizar(codigo))
				.isInstanceOf(CodigoInvalidoException.class)
				.hasMessageContaining("solo puede tener letras sin acentos, números, guion (-) y guion bajo (_)");
	}

	@Test
	void rechazaMasDe15Caracteres() {
		assertThatThrownBy(() -> validador.normalizar("abcdefghijklmnop"))
				.isInstanceOf(CodigoInvalidoException.class)
				.hasMessage("El código puede tener hasta 15 caracteres.");
	}
}
