package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class EnlaceTest {

	private static final Instant CREATED = Instant.parse("2026-10-05T13:00:00Z");
	private static final Instant EXPIRES = CREATED.plus(Duration.ofMinutes(60));

	private final Enlace enlace = new Enlace("abcde", "https://example.org", CREATED, EXPIRES);

	@Test
	void vigenteAntesDeLos60Minutos() {
		assertThat(enlace.estaVencido(CREATED)).isFalse();
		assertThat(enlace.estaVencido(EXPIRES.minusMillis(1))).isFalse();
	}

	@Test
	void vencidoExactamenteALos60Minutos() {
		assertThat(enlace.estaVencido(EXPIRES)).isTrue();
	}

	@Test
	void vencidoDespuesDeLos60Minutos() {
		assertThat(enlace.estaVencido(EXPIRES.plusSeconds(1))).isTrue();
	}
}
