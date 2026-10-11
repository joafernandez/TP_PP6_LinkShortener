package app.enlace;

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
public class ValidadorUrl {

	static final int LONGITUD_MAXIMA = 2048;
	private static final Set<String> ESQUEMAS_PERMITIDOS = Set.of("http", "https");

	private final URI uriBase;

	public ValidadorUrl(AppProperties propiedades) {
		this.uriBase = URI.create(propiedades.urlBase());
	}

	/**
	 * @throws UrlInvalidaException si la URL no es válida o pertenece al propio servicio
	 */
	public void validar(String url) {
		if (url == null || url.isBlank()) {
			throw new UrlInvalidaException("La URL es obligatoria.");
		}
		if (url.length() > LONGITUD_MAXIMA) {
			throw new UrlInvalidaException("La URL no puede superar los " + LONGITUD_MAXIMA + " caracteres.");
		}
		URI uri = analizar(url);
		if (!uri.isAbsolute() || !ESQUEMAS_PERMITIDOS.contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
			throw new UrlInvalidaException("La URL debe ser absoluta y usar el esquema http o https.");
		}
		if (uri.getHost() == null || uri.getHost().isBlank()) {
			throw new UrlInvalidaException("La URL debe indicar un host.");
		}
		if (perteneceAEsteServicio(uri)) {
			throw new UrlInvalidaException("No se puede acortar una URL del propio servicio.");
		}
	}

	private static URI analizar(String url) {
		try {
			return new URI(url);
		} catch (URISyntaxException e) {
			throw new UrlInvalidaException("La URL no tiene un formato válido.");
		}
	}

	private boolean perteneceAEsteServicio(URI uri) {
		return uri.getScheme().equalsIgnoreCase(uriBase.getScheme())
				&& uri.getHost().equalsIgnoreCase(uriBase.getHost())
				&& puertoEfectivo(uri) == puertoEfectivo(uriBase);
	}

	private static int puertoEfectivo(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}
}
