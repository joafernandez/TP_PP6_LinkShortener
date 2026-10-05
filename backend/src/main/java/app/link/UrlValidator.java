package app.link;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import app.common.config.AppProperties;

/**
 * Criterio de URL válida (ADR-0020) y rechazo de URLs del propio servicio (ADR-0019).
 * No verifica que la URL exista o responda.
 */
@Component
public class UrlValidator {

	static final int MAX_LENGTH = 2048;
	private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

	private final URI baseUri;

	public UrlValidator(AppProperties properties) {
		this.baseUri = URI.create(properties.baseUrl());
	}

	/**
	 * @throws InvalidUrlException si la URL no es válida o pertenece al propio servicio
	 */
	public void validate(String url) {
		if (url == null || url.isBlank()) {
			throw new InvalidUrlException("La URL es obligatoria.");
		}
		if (url.length() > MAX_LENGTH) {
			throw new InvalidUrlException("La URL no puede superar los " + MAX_LENGTH + " caracteres.");
		}
		URI uri = parse(url);
		if (!uri.isAbsolute() || !ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
			throw new InvalidUrlException("La URL debe ser absoluta y usar el esquema http o https.");
		}
		if (uri.getHost() == null || uri.getHost().isBlank()) {
			throw new InvalidUrlException("La URL debe indicar un host.");
		}
		if (belongsToThisService(uri)) {
			throw new InvalidUrlException("No se puede acortar una URL del propio servicio.");
		}
	}

	private static URI parse(String url) {
		try {
			return new URI(url);
		} catch (URISyntaxException e) {
			throw new InvalidUrlException("La URL no tiene un formato válido.");
		}
	}

	private boolean belongsToThisService(URI uri) {
		return uri.getScheme().equalsIgnoreCase(baseUri.getScheme())
				&& uri.getHost().equalsIgnoreCase(baseUri.getHost())
				&& effectivePort(uri) == effectivePort(baseUri);
	}

	private static int effectivePort(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}
}
