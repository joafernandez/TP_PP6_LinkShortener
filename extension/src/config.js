// Configuración de la extensión: dirección del servidor y permiso de host.
// Firefox ofrece `browser` y Chrome ofrece `chrome`; los dos devuelven promesas en MV3.
export const ext = globalThis.browser ?? globalThis.chrome;

const STORAGE_KEY = "backendUrl";

/**
 * Valida lo que escribió la persona y lo normaliza.
 * Devuelve { origin, permission } si es válido o { error } si no.
 * - origin: esquema + host + puerto, sin ruta (por ejemplo http://192.168.1.50:8080).
 * - permission: patrón de host para permissions.request(). No lleva puerto porque
 *   Firefox no lo admite en los patrones; el permiso vale para todos los puertos del host.
 */
export function parseBackendUrl(raw) {
	const text = (raw ?? "").trim();
	if (!text) {
		return { error: "Ingresá la dirección del servidor." };
	}
	let url;
	try {
		url = new URL(text);
	} catch {
		return { error: "Ingresá una dirección completa, por ejemplo http://192.168.1.50:8080." };
	}
	if (url.protocol !== "http:" && url.protocol !== "https:") {
		return { error: "La dirección debe empezar con http:// o https://." };
	}
	return { origin: url.origin, permission: hostPermissionOf(url.origin) };
}

/** Patrón de host (sin puerto) a partir de una dirección ya normalizada. */
export function hostPermissionOf(origin) {
	const url = new URL(origin);
	return `${url.protocol}//${url.hostname}/*`;
}

export async function getBackendUrl() {
	const stored = await ext.storage.local.get(STORAGE_KEY);
	return stored[STORAGE_KEY] ?? null;
}

export async function saveBackendUrl(origin) {
	await ext.storage.local.set({ [STORAGE_KEY]: origin });
}

/** ¿El navegador ya otorgó el permiso para ese host? */
export function hasHostPermission(permission) {
	return ext.permissions.contains({ origins: [permission] });
}
