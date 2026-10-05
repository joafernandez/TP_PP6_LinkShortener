package app.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del sistema como bean, para poder fijar la hora en los tests (ADR-0010).
 * El código obtiene la hora con {@code clock.instant()}, nunca con {@code Instant.now()}.
 */
@Configuration
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
