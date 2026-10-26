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
const userName = document.querySelector("#user-name");
const logoutButton = document.querySelector("#logout");
const sessionStatus = document.querySelector("#session-status");
let pending = false;

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

function showResult(link) {
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
	qr.src = `/api/v1/links/${encodeURIComponent(link.alias)}/qr`;
}

form.addEventListener("submit", async (event) => {
	event.preventDefault();
	if (pending) return;
	pending = true;
	button.disabled = true;
	form.setAttribute("aria-busy", "true");
	clearResult();
	showStatus("Acortando tu dirección…");

	try {
		const response = await fetch("/api/v1/links", {
			method: "POST",
			headers: { "Content-Type": "application/json", "Accept": "application/json" },
			body: JSON.stringify({ url: input.value.trim() })
		});
		const link = await response.json().catch(() => null);
		if (!response.ok) {
			showStatus(link?.detail || "No se pudo crear el enlace. Intentá nuevamente.", "error");
			return;
		}
		if (!link?.alias || !link?.shortUrl || !link?.expiresAt || Number.isNaN(Date.parse(link.expiresAt))) {
			showStatus("El servidor devolvió una respuesta inesperada. Intentá nuevamente.", "error");
			return;
		}
		showResult(link);
		showStatus("Enlace creado. Ya podés compartirlo.", "success");
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
	userName.hidden = true;
	userName.textContent = "";
	logoutButton.hidden = true;
}

function showLoggedIn(user) {
	loginLink.hidden = true;
	userName.textContent = user.name || user.email || "Sesión iniciada";
	userName.hidden = false;
	logoutButton.hidden = false;
}

async function loadSession() {
	try {
		const response = await fetch("/api/v1/me", { headers: { "Accept": "application/json" } });
		if (response.ok) {
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
