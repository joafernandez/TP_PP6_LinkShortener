package app.common.config;

import java.time.Duration;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia del servicio (prefijo {@code app}).
 * Ver docs/arquitectura.md, sección 6, y ADR-0010.
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
		@NotBlank String baseUrl,
		@Valid @NotNull LinkSettings link,
		@Valid @NotNull AliasSettings alias) {

	/** Parámetros de los enlaces. */
	public record LinkSettings(@NotNull Duration ttl) {
	}

	/** Parámetros de generación de alias (ADR-0016 y ADR-0011). */
	public record AliasSettings(
			@Min(1) int length,
			@NotBlank String alphabet,
			@Min(1) int maxAttempts,
			Set<String> reserved) {

		public AliasSettings {
			reserved = reserved == null ? Set.of() : Set.copyOf(reserved);
		}
	}
}
