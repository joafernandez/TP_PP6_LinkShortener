package app.common.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
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
		@NotBlank(message = "app.base-url es obligatoria.") String baseUrl,
		@Valid @NotNull LinkSettings link,
		@Valid @NotNull AliasSettings alias) {

	@AssertTrue(message = "app.base-url debe ser una URL HTTP o HTTPS con un host válido.")
	public boolean isBaseUrlValid() {
		if (baseUrl == null || baseUrl.isBlank()) {
			return true; // NotBlank informa el valor ausente.
		}
		try {
			URI uri = new URI(baseUrl);
			return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					&& uri.getHost() != null && !uri.getHost().isBlank();
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/** Parámetros de los enlaces. */
	public record LinkSettings(@NotNull(message = "app.link.ttl es obligatoria.") Duration ttl) {

		@AssertTrue(message = "app.link.ttl debe ser una duración positiva.")
		public boolean isTtlPositive() {
			return ttl == null || (!ttl.isZero() && !ttl.isNegative());
		}
	}

	/** Parámetros de generación de alias (ADR-0016 y ADR-0011). */
	public record AliasSettings(
			@Min(value = 1, message = "app.alias.length debe ser al menos 1.")
			@Max(value = 16, message = "app.alias.length no puede superar 16.") int length,
			@NotBlank String alphabet,
			@Min(1) int maxAttempts,
			Set<String> reserved) {

		public AliasSettings {
			reserved = reserved == null ? Set.of() : Set.copyOf(reserved);
		}
	}
}
