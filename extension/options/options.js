"use strict";
import { ext, parseBackendUrl, getBackendUrl, saveBackendUrl } from "../src/config.js";

const form = document.querySelector("#options-form");
const input = document.querySelector("#backend-url");
const button = document.querySelector("#save-button");
const status = document.querySelector("#status");

function showStatus(message, kind = "") {
	status.textContent = message;
	status.className = `status ${kind}`;
}

async function saveIfGranted(parsed, permissionRequest) {
	try {
		const granted = await permissionRequest;
		if (!granted) {
			showStatus("No diste el permiso, así que la extensión no podrá conectarse a ese servidor.", "error");
			return;
		}
		await saveBackendUrl(parsed.origin);
		input.value = parsed.origin;
		showStatus("Guardado. La extensión ya puede acortar con ese servidor.", "success");
	} catch {
		showStatus("No se pudo pedir el permiso ni guardar la dirección. Intentá nuevamente.", "error");
	} finally {
		button.disabled = false;
	}
}

form.addEventListener("submit", (event) => {
	event.preventDefault();
	const parsed = parseBackendUrl(input.value);
	if (parsed.error) {
		showStatus(parsed.error, "error");
		return;
	}

	// IMPORTANTE (ADR-0014): permissions.request() se llama directamente acá, antes de
	// cualquier await. Si no, el navegador (sobre todo Firefox) deja de considerarlo una
	// acción de la persona y lo rechaza.
	let permissionRequest;
	try {
		permissionRequest = ext.permissions.request({ origins: [parsed.permission] });
	} catch {
		showStatus("No se pudo pedir el permiso. Revisá la dirección e intentá nuevamente.", "error");
		return;
	}
	button.disabled = true;
	showStatus("Esperando tu respuesta al pedido de permiso…");
	saveIfGranted(parsed, permissionRequest);
});

getBackendUrl().then((saved) => {
	if (saved) input.value = saved;
}).catch(() => {
	showStatus("No se pudo leer la dirección guardada.", "error");
});
