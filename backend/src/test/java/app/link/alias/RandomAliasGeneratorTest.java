package app.link.alias;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import app.common.config.AppProperties.AliasSettings;

class RandomAliasGeneratorTest {

	private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";

	@Test
	void generaAliasDelLargoConfigurado() {
		RandomAliasGenerator generator = generator(5, ALPHABET, Set.of());

		for (int i = 0; i < 1000; i++) {
			assertThat(generator.generate()).hasSize(5);
		}
	}

	@Test
	void usaSoloCaracteresDelAlfabeto() {
		RandomAliasGenerator generator = generator(5, ALPHABET, Set.of());

		for (int i = 0; i < 1000; i++) {
			assertThat(generator.generate()).matches("[" + ALPHABET + "]+");
		}
	}

	@Test
	void noUsaCaracteresAmbiguos() {
		RandomAliasGenerator generator = generator(5, ALPHABET, Set.of());

		for (int i = 0; i < 1000; i++) {
			assertThat(generator.generate()).doesNotContain("0", "O", "o", "1", "I", "l");
		}
	}

	@Test
	void nuncaGeneraPalabrasReservadas() {
		// alfabeto de 2 letras y largo 1: solo puede salir "a" o "b"; "a" está reservada
		RandomAliasGenerator generator = generator(1, "ab", Set.of("a"));

		for (int i = 0; i < 200; i++) {
			assertThat(generator.generate()).isEqualTo("b");
		}
	}

	@Test
	void generaAliasDistintos() {
		RandomAliasGenerator generator = generator(5, ALPHABET, Set.of());
		Set<String> generated = new HashSet<>();

		for (int i = 0; i < 1000; i++) {
			generated.add(generator.generate());
		}

		// 56^5 ≈ 550 millones de combinaciones: 1000 alias deberían ser prácticamente todos distintos
		assertThat(generated).hasSizeGreaterThan(990);
	}

	@Test
	void esDeterministicoConLaMismaSemilla() {
		AliasSettings settings = settings(5, ALPHABET, Set.of());
		RandomAliasGenerator a = new RandomAliasGenerator(settings, new Random(42));
		RandomAliasGenerator b = new RandomAliasGenerator(settings, new Random(42));

		assertThat(a.generate()).isEqualTo(b.generate());
	}

	private static RandomAliasGenerator generator(int length, String alphabet, Set<String> reserved) {
		return new RandomAliasGenerator(settings(length, alphabet, reserved), new SecureRandom());
	}

	private static AliasSettings settings(int length, String alphabet, Set<String> reserved) {
		return new AliasSettings(length, alphabet, 10, reserved);
	}
}
