"use strict";

const form = document.querySelector("#shorten-form");
const input = document.querySelector("#url");
const button = document.querySelector("#shorten-button");
const status = document.querySelector("#status");
const result = document.querySelector("#result");
const shortUrl = document.querySelector("#short-url");
const expiresAt = document.querySelector("#expires-at");
const qr = document.querySelector("#qr");
const qrStatus = document.querySelector("#qr-status");
const loginLink = document.querySelector("#login");
const loginQrButton = document.querySelector("#login-qr");
const userName = document.querySelector("#user-name");
const logoutButton = document.querySelector("#logout");
const sessionStatus = document.querySelector("#session-status");
const namespaceCard = document.querySelector("#namespace-card");
const namespaceForm = document.querySelector("#namespace-form");
const namespaceInput = document.querySelector("#namespace");
const namespaceButton = document.querySelector("#namespace-button");
const namespaceCurrent = document.querySelector("#namespace-current");
const namespaceStatus = document.querySelector("#namespace-status");
const codeField = document.querySelector("#code-field");
const codeInput = document.querySelector("#code");
const codePrefix = document.querySelector("#code-prefix");
let pending = false;
// Usuario de GET /api/v1/me, o null sin sesión. Con espacio, los enlaces se crean dentro de él (ADR-0032).
let currentUser = null;

input.addEventListener("input", () => input.setCustomValidity(""));
input.addEventListener("invalid", () => {
	input.setCustomValidity(input.validity.valueMissing
		? "Ingresá la dirección que querés acortar."
		: "Ingresá una dirección completa, por ejemplo https://ejemplo.com.");
});

function showStatus(message, kind = "") {
	status.textContent = message;
	status.className = `status ${kind}`;
}

function clearResult() {
	result.hidden = true;
	shortUrl.textContent = "";
	shortUrl.removeAttribute("href");
	qr.onload = null;
	qr.onerror = null;
	qr.removeAttribute("src");
	qr.hidden = true;
	qrStatus.textContent = "";
}

function showResult(link, namespace) {
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
		qrStatus.textContent = "Escaneá para abrir tu enlace.";
	};
	qr.onerror = () => {
		qr.hidden = true;
		qrStatus.textContent = "No se pudo cargar el QR. Podés seguir usando el enlace corto mientras esté vigente.";
	};
	qr.src = namespace
		? `/api/v1/namespaces/${encodeURIComponent(namespace)}/links/${encodeURIComponent(link.alias)}/qr`
		: `/api/v1/links/${encodeURIComponent(link.alias)}/qr`;
}

/** Sin sesión: alta anónima. Con sesión y espacio: alta en el espacio, con el código opcional y el token CSRF. */
function createLinkRequest(namespace) {
	const url = input.value.trim();
	if (!namespace) {
		return fetch("/api/v1/links", {
			method: "POST",
			headers: { "Content-Type": "application/json", "Accept": "application/json" },
			body: JSON.stringify({ url })
		});
	}
	const code = codeInput.value.trim();
	return fetch("/api/v1/me/links", {
		method: "POST",
		headers: { "Content-Type": "application/json", "Accept": "application/json", "X-XSRF-TOKEN": xsrfToken() },
		body: JSON.stringify(code ? { url, code } : { url })
	});
}

form.addEventListener("submit", async (event) => {
	event.preventDefault();
	if (pending) return;
	if (currentUser && !currentUser.namespace) {
		clearResult();
		showStatus("Elegí tu espacio antes de crear enlaces.", "error");
		namespaceInput.focus();
		return;
	}
	const namespace = currentUser?.namespace || null;
	pending = true;
	button.disabled = true;
	form.setAttribute("aria-busy", "true");
	clearResult();
	showStatus("Acortando tu dirección…");

	try {
		const response = await createLinkRequest(namespace);
		const link = await response.json().catch(() => null);
		if (!response.ok) {
			showStatus(link?.detail || "No se pudo crear el enlace. Intentá nuevamente.", "error");
			return;
		}
		if (!link?.alias || !link?.shortUrl || !link?.expiresAt || Number.isNaN(Date.parse(link.expiresAt))) {
			showStatus("El servidor devolvió una respuesta inesperada. Intentá nuevamente.", "error");
			return;
		}
		showResult(link, namespace);
		showStatus("Enlace creado. Ya podés compartirlo.", "success");
		codeInput.value = "";
	} catch {
		showStatus("No se pudo conectar con el servidor. Comprobá tu conexión e intentá nuevamente.", "error");
	} finally {
		pending = false;
		button.disabled = false;
		form.removeAttribute("aria-busy");
	}
});

// Sesión con Google (ADR-0030). Sin sesión, la página funciona igual que en la Etapa 1.

function showSessionStatus(message, kind = "") {
	sessionStatus.textContent = message;
	sessionStatus.className = `status ${kind}`;
}

function showLoggedOut() {
	loginLink.hidden = false;
	loginQrButton.hidden = false;
	userName.hidden = true;
	userName.textContent = "";
	logoutButton.hidden = true;
	namespaceCard.hidden = true;
	namespaceStatus.textContent = "";
	currentUser = null;
	codeField.hidden = true;
	codeInput.value = "";
}

function showLoggedIn(user) {
	currentUser = user;
	loginLink.hidden = true;
	loginQrButton.hidden = true;
	userName.textContent = user.name || user.email || "Sesión iniciada";
	userName.hidden = false;
	logoutButton.hidden = false;
	showNamespace(user.namespace);
	namespaceCard.hidden = false;
}

function showNamespace(namespace) {
	namespaceCurrent.textContent = namespace
		? `Tu espacio actual es «${namespace}».`
		: "Todavía no elegiste tu espacio. Elegilo para crear tus propios enlaces.";
	namespaceInput.value = namespace || "";
	if (currentUser) {
		currentUser.namespace = namespace;
	}
	codeField.hidden = !namespace;
	codePrefix.textContent = namespace ? `${location.host}/${namespace}/` : "";
}

function showNamespaceStatus(message, kind = "") {
	namespaceStatus.textContent = message;
	namespaceStatus.className = `status ${kind}`;
}

namespaceInput.addEventListener("input", () => namespaceInput.setCustomValidity(""));
namespaceInput.addEventListener("invalid", () => {
	namespaceInput.setCustomValidity("Escribí el nombre de tu espacio.");
});

namespaceForm.addEventListener("submit", async (event) => {
	event.preventDefault();
	namespaceButton.disabled = true;
	showNamespaceStatus("Guardando tu espacio…");
	try {
		const response = await fetch("/api/v1/me/namespace", {
			method: "PUT",
			headers: {
				"Content-Type": "application/json",
				"Accept": "application/json",
				"X-XSRF-TOKEN": xsrfToken()
			},
			body: JSON.stringify({ namespace: namespaceInput.value.trim() })
		});
		const body = await response.json().catch(() => null);
		if (!response.ok) {
			showNamespaceStatus(body?.detail || "No se pudo guardar tu espacio. Recargá la página e intentá nuevamente.", "error");
			return;
		}
		showNamespace(body.namespace);
		showNamespaceStatus("Espacio guardado.", "success");
	} catch {
		showNamespaceStatus("No se pudo conectar con el servidor. Comprobá tu conexión e intentá nuevamente.", "error");
	} finally {
		namespaceButton.disabled = false;
	}
});

/** Si el celular vino del QR y pasó por Google, vuelve a la pantalla donde se autoriza (ADR-0033). */
function volverAlLoginConQr() {
	try {
		if (sessionStorage.getItem("qrLoginPendiente")) {
			location.replace("/qr-login.html");
		}
	} catch {
		// sin almacenamiento: hay que volver a escanear el QR
	}
}

async function loadSession() {
	try {
		const response = await fetch("/api/v1/me", { headers: { "Accept": "application/json" } });
		if (response.ok) {
			volverAlLoginConQr();
			showLoggedIn(await response.json());
			return;
		}
	} catch {
		// sin conexión: se ofrece iniciar sesión igual
	}
	showLoggedOut();
}

/** Spring Security deja el token CSRF en la cookie XSRF-TOKEN y lo espera en el encabezado X-XSRF-TOKEN. */
function xsrfToken() {
	const cookie = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
	return cookie ? decodeURIComponent(cookie.slice("XSRF-TOKEN=".length)) : "";
}

logoutButton.addEventListener("click", async () => {
	logoutButton.disabled = true;
	try {
		const response = await fetch("/logout", { method: "POST", headers: { "X-XSRF-TOKEN": xsrfToken() } });
		if (!response.ok) {
			showSessionStatus("No se pudo cerrar la sesión. Recargá la página e intentá nuevamente.", "error");
			return;
		}
		showLoggedOut();
		showSessionStatus("Cerraste la sesión.", "success");
	} catch {
		showSessionStatus("No se pudo conectar con el servidor para cerrar la sesión.", "error");
	} finally {
		logoutButton.disabled = false;
	}
});

// Si el login con Google falla o se cancela, el servidor vuelve a /?login=error sin crear la sesión.
if (new URLSearchParams(location.search).get("login") === "error") {
	showSessionStatus("No se pudo iniciar sesión con Google, o se canceló. Podés seguir acortando sin iniciar sesión.", "error");
	history.replaceState(null, "", location.pathname);
}

loadSession();
