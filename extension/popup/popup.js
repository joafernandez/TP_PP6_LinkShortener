"use strict";
import { ext, getBackendUrl, hostPermissionOf, hasHostPermission } from "../src/config.js";
import { createLink, currentUser, qrUrlOf } from "../src/api.js";

const status = document.querySelector("#status");
const optionsButton = document.querySelector("#open-options");
const result = document.querySelector("#result");
const shortUrl = document.querySelector("#short-url");
const expiresAt = document.querySelector("#expires-at");
const qr = document.querySelector("#qr");
const qrStatus = document.querySelector("#qr-status");

function showStatus(message, kind = "") {
	status.textContent = message;
	status.className = `status ${kind}`;
}

function showError(message, offerOptions = false) {
	showStatus(message, "error");
	optionsButton.hidden = !offerOptions;
}

// Solo una comprobación de comodidad: la validación real la hace el backend.
function isShortenable(url) {
	return typeof url === "string" && /^https?:\/\//i.test(url);
}

async function currentTabUrl() {
	const [tab] = await ext.tabs.query({ active: true, currentWindow: true });
	return tab?.url ?? null;
}

function showResult(link, backendUrl, namespace) {
	shortUrl.textContent = link.shortUrl;
	shortUrl.href = link.shortUrl;
	expiresAt.dateTime = link.expiresAt;
	expiresAt.textContent = new Intl.DateTimeFormat("es-AR", {
		dateStyle: "short", timeStyle: "medium"
	}).format(new Date(link.expiresAt));
	result.hidden = false;
	qrStatus.textContent = "Cargando el código QR…";
	qr.onload = () => {
		qr.hidden = false;
		qrStatus.textContent = "Escaneá para abrir el enlace.";
	};
	qr.onerror = () => {
		qr.hidden = true;
		qrStatus.textContent = "No se pudo cargar el QR. Podés seguir usando el enlace corto mientras esté vigente.";
	};
	qr.src = qrUrlOf(backendUrl, link.alias, namespace);
}

/** Token CSRF: la cookie XSRF-TOKEN que el servidor dejó en el navegador (hace falta el permiso "cookies"). */
async function csrfTokenOf(backendUrl) {
	try {
		const cookie = await ext.cookies.get({ url: backendUrl, name: "XSRF-TOKEN" });
		return cookie?.value ?? "";
	} catch {
		return "";
	}
}

async function shortenCurrentPage() {
	showStatus("Acortando esta página…");

	const tabUrl = await currentTabUrl();
	if (!isShortenable(tabUrl)) {
		showError("Esta página no se puede acortar. Abrí una que empiece con http:// o https://.");
		return;
	}

	const backendUrl = await getBackendUrl();
	if (!backendUrl) {
		showError("Falta configurar la dirección del servidor.", true);
		return;
	}
	if (!(await hasHostPermission(hostPermissionOf(backendUrl)))) {
		showError("La extensión no tiene permiso para conectarse al servidor configurado. Abrí las opciones y guardá la dirección de nuevo.", true);
		return;
	}

	try {
		// Con sesión iniciada en la web y un espacio elegido, el enlace se crea en ese espacio.
		// Si no hay sesión, o no hay espacio, se crea anónimo como siempre.
		const user = await currentUser(backendUrl);
		const namespace = user?.namespace || null;
		const csrfToken = namespace ? await csrfTokenOf(backendUrl) : "";
		if (namespace && !csrfToken) {
			showError("No se pudo leer la sesión del navegador. Abrí la página del servidor, recargala e intentá nuevamente.");
			return;
		}
		const link = await createLink(backendUrl, tabUrl, { namespace, csrfToken });
		showResult(link, backendUrl, namespace);
		showStatus(namespace
			? `Enlace creado en tu espacio «${namespace}».`
			: user
				? "Enlace creado. Elegí tu espacio en la web para crear enlaces propios."
				: "Enlace creado.", "success");
	} catch (error) {
		showError(error.message);
	}
}

optionsButton.addEventListener("click", () => {
	ext.runtime.openOptionsPage();
});

shortenCurrentPage().catch(() => {
	showError("Ocurrió un error inesperado. Intentá nuevamente.");
});
