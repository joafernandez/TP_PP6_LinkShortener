package app.espacio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import app.usuario.Usuario;

class EspacioTest {

	@ParameterizedTest
	@ValueSource(strings = { "melina", "Melina", "MELINA", "mElInA" })
	void normalizaElNombreAMinusculas(String nombre) {
		Espacio espacio = new Espacio(nombre, new Usuario("google-1", null, null, Instant.EPOCH));

		assertThat(espacio.getNombre()).isEqualTo("melina");
	}

	@ParameterizedTest
	@ValueSource(strings = { "JOANA23", "Joana23" })
	void conservaLosNumeros(String nombre) {
		assertThat(Espacio.normalizar(nombre)).isEqualTo("joana23");
	}
}
