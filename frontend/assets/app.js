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
const namespacePrevious = document.querySelector("#namespace-previous");
const myLinks = document.querySelector("#my-links");
const myLinksList = document.querySelector("#my-links-list");
const myLinksStatus = document.querySelector("#my-links-status");
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
		if (namespace) {
			loadMyLinks();
		}
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
	showPreviousNamespaces([]);
	myLinks.hidden = true;
	myLinksList.replaceChildren();
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
	showPreviousNamespaces(user.previousNamespaces || []);
	loadMyLinks();
	namespaceCard.hidden = false;
}

// Nombres anteriores en transición (ADR-0034): el aviso es solo para el dueño y muestra el tiempo real
// que le queda a cada uno, calculado con el vencimiento que informa el servidor.
let previousNamespaces = [];
let expiredNamespaces = [];
let previousTimer = null;

function showPreviousNamespaces(previous) {
	previousNamespaces = previous.filter((anterior) => !Number.isNaN(Date.parse(anterior.expiresAt)));
	expiredNamespaces = [];
	renderPreviousNamespaces();
	clearInterval(previousTimer);
	previousTimer = previousNamespaces.length ? setInterval(renderPreviousNamespaces, 15000) : null;
}

function renderPreviousNamespaces() {
	const now = Date.now();
	expiredNamespaces.push(...previousNamespaces.filter((anterior) => Date.parse(anterior.expiresAt) <= now));
	previousNamespaces = previousNamespaces.filter((anterior) => Date.parse(anterior.expiresAt) > now);
	if (!previousNamespaces.length) {
		clearInterval(previousTimer);
		previousTimer = null;
	}
	const current = currentUser?.namespace;
	const messages = previousNamespaces.map((anterior) => {
		const minutes = Math.ceil((Date.parse(anterior.expiresAt) - now) / 60000);
		return `Tu espacio anterior /${anterior.namespace} seguirá funcionando durante ${minutes} `
			+ `${minutes === 1 ? "minuto" : "minutos"}. Después de ese plazo, los enlaces deberán utilizar /${current}.`;
	}).concat(expiredNamespaces.map((anterior) =>
		`Tu espacio anterior /${anterior.namespace} ya expiró. Tus enlaces funcionan solo con /${current}.`));
	namespacePrevious.replaceChildren(...messages.map((message) => {
		const paragraph = document.createElement("p");
		paragraph.className = "hint";
		paragraph.textContent = message;
		return paragraph;
	}));
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
		const changed = currentUser?.namespace && currentUser.namespace !== body.namespace;
		showNamespace(body.namespace);
		showPreviousNamespaces(body.previousNamespaces || []);
		loadMyLinks();
		showNamespaceStatus(changed ? "Cambio de espacio realizado." : "Espacio guardado.", "success");
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

// «Mis enlaces»: los enlaces del espacio del usuario con sesión, del más reciente al más antiguo.
// Se recarga al iniciar sesión, al crear un enlace en el espacio y al guardar el espacio.

const dateFormat = new Intl.DateTimeFormat("es-AR", { dateStyle: "short", timeStyle: "short" });

async function loadMyLinks() {
	myLinks.hidden = false;
	if (!currentUser?.namespace) {
		myLinksList.replaceChildren();
		myLinksStatus.textContent = "Elegí tu espacio para empezar a crear tus enlaces.";
		return;
	}
	try {
		const response = await fetch("/api/v1/me/links", { headers: { "Accept": "application/json" } });
		const links = await response.json().catch(() => null);
		if (!response.ok || !Array.isArray(links)) {
			myLinksStatus.textContent = "No se pudieron cargar tus enlaces. Recargá la página para intentar nuevamente.";
			return;
		}
		myLinksStatus.textContent = links.length ? "" : "Todavía no creaste enlaces en tu espacio.";
		myLinksList.replaceChildren(...links.map(myLinkItem));
	} catch {
		myLinksStatus.textContent = "No se pudo conectar con el servidor para cargar tus enlaces.";
	}
}

function myLinkItem(link) {
	const active = Date.parse(link.expiresAt) > Date.now();
	const item = element("li", "my-link");

	const head = element("div", "my-link-head");
	const short = element("a", "my-link-short", link.shortUrl);
	short.href = link.shortUrl;
	short.target = "_blank";
	short.rel = "noopener noreferrer";
	head.append(short, element("span", `badge ${active ? "active" : "expired"}`, active ? "Vigente" : "Vencido"));

	const original = element("p", "my-link-original", link.originalUrl);
	const dates = element("p", "my-link-dates",
		`Creado: ${dateFormat.format(new Date(link.createdAt))} · Vence: ${dateFormat.format(new Date(link.expiresAt))}`);

	const actions = element("div", "my-link-actions");
	const copy = element("button", "secondary", "Copiar");
	copy.type = "button";
	copy.addEventListener("click", async () => {
		try {
			await navigator.clipboard.writeText(link.shortUrl);
			copy.textContent = "¡Copiado!";
		} catch {
			copy.textContent = "No se pudo copiar";
		}
		setTimeout(() => { copy.textContent = "Copiar"; }, 2000);
	});
	const open = element("a", "secondary", "Abrir");
	open.href = link.shortUrl;
	open.target = "_blank";
	open.rel = "noopener noreferrer";
	const showQr = element("button", "secondary", "Ver QR");
	showQr.type = "button";
	const qrImage = element("img", "my-link-qr");
	qrImage.alt = `Código QR de ${link.shortUrl}`;
	qrImage.hidden = true;
	const qrMessage = element("p", "hint");
	showQr.addEventListener("click", () => {
		if (!qrImage.hidden || qrMessage.textContent) {
			qrImage.hidden = true;
			qrMessage.textContent = "";
			showQr.textContent = "Ver QR";
			return;
		}
		showQr.textContent = "Ocultar QR";
		qrImage.onload = () => { qrImage.hidden = false; };
		qrImage.onerror = () => { qrMessage.textContent = "El QR no está disponible: el enlace venció."; };
		qrImage.src = `/api/v1/namespaces/${encodeURIComponent(currentUser.namespace)}/links/`
			+ `${encodeURIComponent(link.alias)}/qr?t=${Date.now()}`;
	});
	actions.append(copy, open, showQr);

	item.append(head, original, dates, actions, qrImage, qrMessage);
	return item;
}

/** Crea un elemento con clase y texto; el texto se asigna con textContent para no interpretar HTML. */
function element(tag, className, text) {
	const node = document.createElement(tag);
	node.className = className;
	if (text !== undefined) {
		node.textContent = text;
	}
	return node;
}

loadSession();
