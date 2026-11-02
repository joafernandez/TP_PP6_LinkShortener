// Cliente de la API REST (contrato: docs/api/openapi.yaml). Sin lógica de negocio.

/** Respuesta de error del servidor (ProblemDetail) o respuesta inesperada. */
export class ApiError extends Error {}

/** No se pudo hablar con el servidor. */
export class NetworkError extends Error {}

/** GET /api/v1/me con la cookie de sesión del navegador: el usuario, o null si no hay sesión o falla. */
export async function currentUser(backendUrl) {
	try {
		const response = await fetch(`${backendUrl}/api/v1/me`, {
			headers: { "Accept": "application/json" },
			credentials: "include"
		});
		return response.ok ? await response.json() : null;
	} catch {
		return null;
	}
}

/**
 * Crea el enlace. Sin espacio: POST /api/v1/links (anónimo). Con espacio: POST /api/v1/me/links con la sesión
 * y el token CSRF (`csrfToken`, leído de la cookie XSRF-TOKEN). Devuelve el EnlaceResponse del servidor.
 */
export async function createLink(backendUrl, originalUrl, { namespace = null, csrfToken = "" } = {}) {
	let response;
	try {
		response = await fetch(namespace ? `${backendUrl}/api/v1/me/links` : `${backendUrl}/api/v1/links`, {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				"Accept": "application/json",
				...(namespace ? { "X-XSRF-TOKEN": csrfToken } : {})
			},
			credentials: namespace ? "include" : "omit",
			body: JSON.stringify({ url: originalUrl })
		});
	} catch {
		throw new NetworkError("No se pudo conectar con el servidor. Comprobá que esté encendido y que la dirección configurada sea correcta.");
	}
	const body = await response.json().catch(() => null);
	if (!response.ok) {
		throw new ApiError(body?.detail || "No se pudo crear el enlace. Intentá nuevamente.");
	}
	if (!body?.alias || !body?.shortUrl || !body?.expiresAt || Number.isNaN(Date.parse(body.expiresAt))) {
		throw new ApiError("El servidor devolvió una respuesta inesperada. Intentá nuevamente.");
	}
	return body;
}

/** URL de la imagen PNG del QR: del enlace anónimo o, con espacio, del enlace dentro del espacio. */
export function qrUrlOf(backendUrl, alias, namespace = null) {
	return namespace
		? `${backendUrl}/api/v1/namespaces/${encodeURIComponent(namespace)}/links/${encodeURIComponent(alias)}/qr`
		: `${backendUrl}/api/v1/links/${encodeURIComponent(alias)}/qr`;
}
