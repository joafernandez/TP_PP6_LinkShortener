// Cliente de la API REST (contrato: docs/api/openapi.yaml). Sin lógica de negocio.

/** Respuesta de error del servidor (ProblemDetail) o respuesta inesperada. */
export class ApiError extends Error {}

/** No se pudo hablar con el servidor. */
export class NetworkError extends Error {}

/** POST /api/v1/links: devuelve el LinkResponse del servidor. */
export async function createLink(backendUrl, originalUrl) {
	let response;
	try {
		response = await fetch(`${backendUrl}/api/v1/links`, {
			method: "POST",
			headers: { "Content-Type": "application/json", "Accept": "application/json" },
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

/** URL de la imagen PNG del QR: GET /api/v1/links/{alias}/qr. */
export function qrUrlOf(backendUrl, alias) {
	return `${backendUrl}/api/v1/links/${encodeURIComponent(alias)}/qr`;
}
