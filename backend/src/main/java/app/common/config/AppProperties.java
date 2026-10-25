package app.common.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia del servicio (prefijo {@code app}).
 * Ver docs/arquitectura.md, sección 6, y ADR-0010.
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
		@Name("base-url") @NotBlank(message = "app.base-url es obligatoria.") String urlBase,
		@Name("link") @Valid @NotNull EnlaceSettings enlace,
		@Valid @NotNull AliasSettings alias) {

	@AssertTrue(message = "app.base-url debe ser una URL HTTP o HTTPS con un host válido.")
	public boolean isUrlBaseValida() {
		if (urlBase == null || urlBase.isBlank()) {
			return true; // NotBlank informa el valor ausente.
		}
		try {
			URI uri = new URI(urlBase);
			return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					&& uri.getHost() != null && !uri.getHost().isBlank();
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/** Parámetros de los enlaces. */
	public record EnlaceSettings(@NotNull(message = "app.link.ttl es obligatoria.") Duration ttl) {

		@AssertTrue(message = "app.link.ttl debe ser una duración positiva.")
		public boolean isTtlPositivo() {
			return ttl == null || (!ttl.isZero() && !ttl.isNegative());
		}
	}

	/** Parámetros de generación de alias (ADR-0026 y ADR-0011). */
	public record AliasSettings(
			@Name("alphabet") @NotBlank String alfabeto,
			@Name("max-attempts") @Min(1) int maxIntentos,
			@Name("reserved") Set<String> reservadas) {

		public AliasSettings {
			reservadas = reservadas == null ? Set.of() : Set.copyOf(reservadas);
		}
	}
}
