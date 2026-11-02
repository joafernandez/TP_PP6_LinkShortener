"use strict";

// Página del celular para el login con QR (ADR-0033). Recibe el código en ?c=..., pide iniciar sesión con
// Google si hace falta y deja al usuario autorizar o rechazar el ingreso en la computadora.

const CLAVE_PENDIENTE = "qrLoginPendiente";
const info = document.querySelector("#phone-info");
const loginLink = document.querySelector("#phone-login");
const question = document.querySelector("#phone-question");
const userLine = document.querySelector("#phone-user");
const authorizeButton = document.querySelector("#phone-authorize");
const rejectButton = document.querySelector("#phone-reject");
const status = document.querySelector("#phone-status");

function showStatus(message, kind = "") {
	status.textContent = message;
	status.className = `status ${kind}`;
}

function xsrfToken() {
	const cookie = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
	return cookie ? decodeURIComponent(cookie.slice("XSRF-TOKEN=".length)) : "";
}

/** El código viaja en la dirección; si no, se recupera el guardado antes de ir a Google. */
function obtenerCodigo() {
	const deLaDireccion = new URLSearchParams(location.search).get("c");
	if (deLaDireccion) return deLaDireccion;
	try {
		return sessionStorage.getItem(CLAVE_PENDIENTE);
	} catch {
		return null;
	}
}

function olvidarCodigo() {
	try {
		sessionStorage.removeItem(CLAVE_PENDIENTE);
	} catch {
		// sin almacenamiento: no hay nada que olvidar
	}
}

function recordarCodigo(codigo) {
	try {
		sessionStorage.setItem(CLAVE_PENDIENTE, codigo);
	} catch {
		// sin almacenamiento: después de Google habrá que volver a escanear el QR
	}
}

async function responder(codigo, accion) {
	authorizeButton.disabled = true;
	rejectButton.disabled = true;
	showStatus(accion === "authorize" ? "Autorizando…" : "Rechazando…");
	try {
		const response = await fetch(`/api/v1/auth/qr-logins/${encodeURIComponent(codigo)}/${accion}`, {
			method: "POST",
			headers: { "Accept": "application/json", "X-XSRF-TOKEN": xsrfToken() }
		});
		if (response.ok) {
			question.hidden = true;
			olvidarCodigo();
			info.textContent = "";
			showStatus(accion === "authorize"
				? "Listo: ya podés volver a tu computadora, que está iniciando sesión."
				: "Rechazaste el ingreso. La computadora no va a iniciar sesión.", "success");
			return;
		}
		const body = await response.json().catch(() => null);
		showStatus(body?.detail || "No se pudo completar la acción. Escaneá un código nuevo desde tu computadora.", "error");
		if (response.status === 404 || response.status === 409) {
			question.hidden = true;
			olvidarCodigo();
		} else {
			authorizeButton.disabled = false;
			rejectButton.disabled = false;
		}
	} catch {
		showStatus("No se pudo conectar con el servidor. Comprobá tu conexión e intentá nuevamente.", "error");
		authorizeButton.disabled = false;
		rejectButton.disabled = false;
	}
}

async function iniciar() {
	const codigo = obtenerCodigo();
	if (!codigo) {
		info.textContent = "";
		showStatus("Este enlace no tiene un código de inicio de sesión. Escaneá el QR que muestra tu computadora.", "error");
		return;
	}
	if (location.search) {
		history.replaceState(null, "", location.pathname); // el código no queda en la barra ni en el historial
	}
	let usuario = null;
	try {
		const response = await fetch("/api/v1/me", { headers: { "Accept": "application/json" } });
		if (response.ok) usuario = await response.json();
	} catch {
		info.textContent = "";
		showStatus("No se pudo conectar con el servidor. Comprobá tu conexión e intentá nuevamente.", "error");
		return;
	}
	if (!usuario) {
		recordarCodigo(codigo);
		info.textContent = "Para autorizar el ingreso en tu computadora, primero iniciá sesión con Google en este celular.";
		loginLink.hidden = false;
		return;
	}
	info.textContent = "";
	userLine.textContent = `Vas a autorizar el ingreso como ${usuario.name || usuario.email}.`;
	question.hidden = false;
	authorizeButton.addEventListener("click", () => responder(codigo, "authorize"));
	rejectButton.addEventListener("click", () => responder(codigo, "reject"));
}

iniciar();
