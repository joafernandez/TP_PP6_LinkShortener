package app.espacio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;
import app.common.config.AppProperties.EnlaceSettings;
import app.common.config.AppProperties.EspacioSettings;

class ValidadorEspacioTest {

	// en mayúsculas a propósito: la configuración se normaliza igual que los nombres
	private final ValidadorEspacio validador = new ValidadorEspacio(new AppProperties("http://short.test",
			new EnlaceSettings(Duration.ofMinutes(60)), new AliasSettings("abc", 10, Set.of()),
			new EspacioSettings(Set.of("API", "v3"))));

	@ParameterizedTest
	@ValueSource(strings = { "melina", "Melina", "MELINA", "joana23", "a", "123", "abcdefghijklmno" })
	void aceptaLetrasYNumerosHasta15CaracteresYDevuelveMinusculas(String nombre) {
		assertThat(validador.normalizar(nombre)).isEqualTo(nombre.toLowerCase());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void pideEscribirUnNombre(String nombre) {
		assertThatThrownBy(() -> validador.normalizar(nombre))
				.isInstanceOf(NombreEspacioInvalidoException.class)
				.hasMessage("Escribí el nombre de tu espacio.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "mel ina", "mel-ina", "mel_ina", "melina!", "joaná", "mel/ina", " melina" })
	void rechazaEspaciosSimbolosYAcentos(String nombre) {
		assertThatThrownBy(() -> validador.normalizar(nombre))
				.isInstanceOf(NombreEspacioInvalidoException.class)
				.hasMessageContaining("solo puede tener letras sin acentos y números");
	}

	@Test
	void rechazaMasDe15Caracteres() {
		assertThatThrownBy(() -> validador.normalizar("abcdefghijklmnop"))
				.isInstanceOf(NombreEspacioInvalidoException.class)
				.hasMessage("El espacio puede tener hasta 15 caracteres.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "api", "API", "v3" })
	void rechazaLosNombresReservadosSinDistinguirMayusculas(String nombre) {
		assertThatThrownBy(() -> validador.normalizar(nombre))
				.isInstanceOf(NombreEspacioInvalidoException.class)
				.hasMessageContaining("reservado");
	}
}
