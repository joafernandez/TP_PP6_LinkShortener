package app.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class LinkTest {

	private static final Instant CREATED = Instant.parse("2026-10-05T13:00:00Z");
	private static final Instant EXPIRES = CREATED.plus(Duration.ofMinutes(60));

	private final Link link = new Link("abcde", "https://example.org", CREATED, EXPIRES);

	@Test
	void vigenteAntesDeLos60Minutos() {
		assertThat(link.isExpired(CREATED)).isFalse();
		assertThat(link.isExpired(EXPIRES.minusMillis(1))).isFalse();
	}

	@Test
	void vencidoExactamenteALos60Minutos() {
		assertThat(link.isExpired(EXPIRES)).isTrue();
	}

	@Test
	void vencidoDespuesDeLos60Minutos() {
		assertThat(link.isExpired(EXPIRES.plusSeconds(1))).isTrue();
	}
}
