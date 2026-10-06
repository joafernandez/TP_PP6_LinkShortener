package app.support;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Reemplaza el reloj del sistema por un {@link MutableClock} en los tests de integración. */
@TestConfiguration
public class TestClockConfig {

	public static final Instant T0 = Instant.parse("2026-10-05T13:00:00Z");

	@Bean
	@Primary
	MutableClock testClock() {
		return new MutableClock(T0);
	}
}
