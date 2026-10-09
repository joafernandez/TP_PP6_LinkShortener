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
