"use strict";

// Login con QR desde la computadora (ADR-0033): muestra el QR, consulta cada 2 segundos y, si el celular
// autoriza, canjea el pedido por la sesión. El secreto vive solo en memoria de esta pestaña.

const INTERVALO_MS = 2000;
const startButton = document.querySelector("#login-qr");
const panel = document.querySelector("#qr-login-card");
const image = document.querySelector("#qr-login-image");
const message = document.querySelector("#qr-login-status");
const retryButton = document.querySelector("#qr-login-retry");
const cancelButton = document.querySelector("#qr-login-cancel");

let pedido = null; // { code, secret }
let temporizador = null;

function mostrar(texto, tipo = "") {
	message.textContent = texto;
	message.className = `status ${tipo}`;
}

function detener() {
	clearTimeout(temporizador);
	temporizador = null;
	pedido = null;
}

function terminar(texto, tipo = "error") {
	detener();
	image.removeAttribute("src");
	image.hidden = true;
	retryButton.hidden = false;
	cancelButton.hidden = false;
	mostrar(texto, tipo);
}

async function comenzar() {
	detener();
	panel.hidden = false;
	image.hidden = true;
	retryButton.hidden = true;
	cancelButton.hidden = false;
	mostrar("Generando el código…");
	try {
		const response = await fetch("/api/v1/auth/qr-logins", { method: "POST", headers: { "Accept": "application/json" } });
		const body = await response.json().catch(() => null);
		if (!response.ok || !body?.code || !body?.secret) {
			terminar(body?.detail || "No se pudo generar el código. Intentá nuevamente.");
			return;
		}
		pedido = { code: body.code, secret: body.secret };
		image.onload = () => { image.hidden = false; };
		image.onerror = () => terminar("No se pudo cargar el código QR. Intentá nuevamente.");
		image.src = `/api/v1/auth/qr-logins/${encodeURIComponent(body.code)}/qr`;
		mostrar("Escaneá el código con tu celular. Vence en 2 minutos.");
		temporizador = setTimeout(consultar, INTERVALO_MS);
	} catch {
		terminar("No se pudo conectar con el servidor. Comprobá tu conexión e intentá nuevamente.");
	}
}

async function consultar() {
	const actual = pedido;
	if (!actual) return;
	try {
		const response = await fetch(`/api/v1/auth/qr-logins/${encodeURIComponent(actual.code)}`, {
			headers: { "Accept": "application/json", "X-Qr-Secret": actual.secret }
		});
		if (pedido !== actual) return; // se canceló o se pidió otro código mientras tanto
		if (!response.ok) {
			terminar("El código ya no es válido. Generá uno nuevo.");
			return;
		}
		const { status } = await response.json();
		if (status === "PENDING") {
			mostrar("Esperando que escanees el código y autorices desde el celular…");
		} else if (status === "AUTHORIZED") {
			await canjear(actual);
			return;
		} else if (status === "REJECTED") {
			terminar("Rechazaste el inicio de sesión desde el celular. Si fue un error, generá un código nuevo.");
			return;
		} else if (status === "EXPIRED") {
			terminar("El código venció. Generá uno nuevo.");
			return;
		} else {
			terminar("El código ya se usó. Generá uno nuevo.");
			return;
		}
	} catch {
		mostrar("Sin conexión con el servidor. Reintentando…", "error");
	}
	if (pedido === actual) temporizador = setTimeout(consultar, INTERVALO_MS);
}

async function canjear(actual) {
	mostrar("Autorizado. Iniciando sesión…", "success");
	try {
		const response = await fetch(`/api/v1/auth/qr-logins/${encodeURIComponent(actual.code)}/session`, {
			method: "POST",
			headers: { "Accept": "application/json", "X-Qr-Secret": actual.secret }
		});
		if (!response.ok) {
			terminar("No se pudo iniciar la sesión: el código venció o ya se usó. Generá uno nuevo.");
			return;
		}
		detener();
		location.reload();
	} catch {
		terminar("No se pudo conectar con el servidor para iniciar la sesión. Generá un código nuevo.");
	}
}

startButton.addEventListener("click", comenzar);
retryButton.addEventListener("click", comenzar);
cancelButton.addEventListener("click", () => {
	detener();
	panel.hidden = true;
	image.removeAttribute("src");
	mostrar("");
});
