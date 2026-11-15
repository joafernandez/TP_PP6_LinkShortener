package app.common.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
		@Valid @NotNull AliasSettings alias,
		@Name("namespace") @Valid EspacioSettings espacio) {

	public AppProperties {
		espacio = espacio == null ? new EspacioSettings(null, null) : espacio;
	}

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

	/**
	 * Parámetros del espacio: nombres que chocan con rutas del sistema (ADR-0029) y cuánto sigue funcionando
	 * un nombre anterior después de cambiarlo (ADR-0034, 60 minutos si no se configura).
	 */
	public record EspacioSettings(
			@Name("reserved") Set<String> reservados,
			@Name("transition") Duration transicion) {

		private static final Duration TRANSICION_POR_DEFECTO = Duration.ofMinutes(60);

		public EspacioSettings {
			reservados = reservados == null ? Set.of()
					: reservados.stream().map(nombre -> nombre.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
			transicion = transicion == null ? TRANSICION_POR_DEFECTO : transicion;
		}

		@AssertTrue(message = "app.namespace.transition debe ser una duración positiva.")
		public boolean isTransicionPositiva() {
			return !transicion.isZero() && !transicion.isNegative();
		}
	}
}
