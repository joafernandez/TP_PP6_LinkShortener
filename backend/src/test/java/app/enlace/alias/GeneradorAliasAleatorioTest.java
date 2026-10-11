package app.enlace.alias;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import app.common.config.AppProperties.AliasSettings;

class GeneradorAliasAleatorioTest {

	private static final String ALFABETO = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";

	@Test
	void generaAliasDelLargoConfigurado() {
		GeneradorAliasAleatorio generador = generador(5, ALFABETO, Set.of());

		for (int i = 0; i < 1000; i++) {
			assertThat(generador.generar()).hasSize(5);
		}
	}

	@Test
	void usaSoloCaracteresDelAlfabeto() {
		GeneradorAliasAleatorio generador = generador(5, ALFABETO, Set.of());

		for (int i = 0; i < 1000; i++) {
			assertThat(generador.generar()).matches("[" + ALFABETO + "]+");
		}
	}

	@Test
	void noUsaCaracteresAmbiguos() {
		GeneradorAliasAleatorio generador = generador(5, ALFABETO, Set.of());

		for (int i = 0; i < 1000; i++) {
			assertThat(generador.generar()).doesNotContain("0", "O", "o", "1", "I", "l");
		}
	}

	@Test
	void nuncaGeneraPalabrasReservadas() {
		// alfabeto de 2 letras y largo 1: solo puede salir "a" o "b"; "a" está reservada
		GeneradorAliasAleatorio generador = generador(1, "ab", Set.of("a"));

		for (int i = 0; i < 200; i++) {
			assertThat(generador.generar()).isEqualTo("b");
		}
	}

	@Test
	void generaAliasDistintos() {
		GeneradorAliasAleatorio generador = generador(5, ALFABETO, Set.of());
		Set<String> generated = new HashSet<>();

		for (int i = 0; i < 1000; i++) {
			generated.add(generador.generar());
		}

		// 56^5 ≈ 550 millones de combinaciones: 1000 alias deberían ser prácticamente todos distintos
		assertThat(generated).hasSizeGreaterThan(990);
	}

	@Test
	void esDeterministicoConLaMismaSemilla() {
		AliasSettings configuracion = configuracion(5, ALFABETO, Set.of());
		GeneradorAliasAleatorio a = new GeneradorAliasAleatorio(configuracion, new Random(42));
		GeneradorAliasAleatorio b = new GeneradorAliasAleatorio(configuracion, new Random(42));

		assertThat(a.generar()).isEqualTo(b.generar());
	}

	private static GeneradorAliasAleatorio generador(int longitud, String alfabeto, Set<String> reservadas) {
		return new GeneradorAliasAleatorio(configuracion(longitud, alfabeto, reservadas), new SecureRandom());
	}

	private static AliasSettings configuracion(int longitud, String alfabeto, Set<String> reservadas) {
		return new AliasSettings(longitud, alfabeto, 10, reservadas);
	}
}
