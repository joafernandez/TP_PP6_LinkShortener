# Extensión Link Shortener (Chrome y Firefox)

Incremento 7 de la Etapa 1 (HU-02, HU-03 y HU-06). Al tocar el ícono, acorta la página que se está viendo y muestra la URL corta, su vencimiento y el código QR. Es un cliente del mismo backend que la página web: no tiene lógica de negocio ([ADR-0014](../docs/adr/0014-extension-mv3-y-permisos-de-host.md)).

## Estructura

| Archivo | Para qué sirve |
|---|---|
| `manifest.json` | Manifest V3, un solo archivo para Chrome y Firefox |
| `popup/` | Ventana que se abre al tocar el ícono: acorta la pestaña activa y muestra el resultado |
| `options/` | Pantalla de opciones: dirección del servidor y pedido de permiso |
| `src/config.js` | Validación de la dirección, almacenamiento y permiso de host |
| `src/api.js` | Llamadas a `POST /api/v1/links` y URL de `GET /api/v1/links/{alias}/qr` |
| `icons/` | Íconos 16, 32, 48 y 128 px |

## Cargarla en el navegador

Antes, levantar el backend (ver el [README principal](../README.md)). Si se usa otro puerto o IP, definir `APP_BASE_URL` con esa dirección, porque con ella se arman las URL cortas.

**Chrome:** abrir `chrome://extensions`, activar **Modo de desarrollador**, tocar **Cargar descomprimida** y elegir la carpeta `extension/`. Chrome puede mostrar una advertencia por la clave `browser_specific_settings`; es de Firefox y no afecta el funcionamiento.

**Firefox (128 o superior):** abrir `about:debugging#/runtime/this-firefox`, tocar **Cargar complemento temporal…** y elegir `extension/manifest.json`. Es temporal: se pierde al cerrar Firefox.

## Configurarla (una vez)

1. Abrir las opciones: botón derecho sobre el ícono → **Opciones** (en Chrome, **Opciones**; en Firefox, **Administrar extensión** → **Opciones**), o el botón **Abrir opciones** que aparece en el popup.
2. Escribir la dirección del servidor, por ejemplo `http://localhost:8081`, y tocar **Guardar**.
3. El navegador pide permiso para conectarse a ese servidor. Aceptar.

La dirección se guarda solo si se acepta el permiso. El permiso se pide por **host, sin puerto** (por ejemplo `http://localhost/*`), porque Firefox no admite puertos en los patrones de host; en la práctica vale para todos los puertos de ese host.

## Usarla

Estando en una página `http://` o `https://`, tocar el ícono: sin otra acción aparecen la URL corta, el vencimiento (hora local) y el QR. Cada vez que se abre el popup se crea un enlace nuevo ([ADR-0017](../docs/adr/0017-misma-url-alias-nuevo.md)).

## Prueba manual (hacer en Chrome y en Firefox)

| # | Pasos | Resultado esperado | Criterio |
|---|---|---|---|
| 1 | Sin configurar, tocar el ícono en una página web | Mensaje "Falta configurar…" y botón **Abrir opciones** | CA-02.3 |
| 2 | En opciones, guardar una dirección vacía o `localhost:8081` (sin esquema) | Mensaje de error y no se guarda nada | CA-03.1 |
| 3 | Guardar `http://localhost:8081` y **rechazar** el permiso | Mensaje "…no podrá conectarse…" y no se guarda | CA-03.2 |
| 4 | Guardar `http://localhost:8081` y **aceptar** el permiso | Mensaje "Guardado" y la dirección queda en el campo | CA-03.1 |
| 5 | En una página `https://…`, tocar el ícono (backend encendido) | URL corta con `{URL base}/{alias}` de 5 caracteres, vencimiento y QR | CA-02.1, CA-06.1 |
| 6 | Abrir la URL corta mostrada | Redirige a la página original | CA-04.1 |
| 7 | Escanear el QR con el celular (red compartida y `APP_BASE_URL` con la IP de la PC) | Abre la URL corta | CA-06.2 |
| 8 | Tocar el ícono en `chrome://extensions` o `about:debugging` | Mensaje "Esta página no se puede acortar" | CA-02.2 |
| 9 | Apagar el backend y tocar el ícono en una página web | Mensaje "No se pudo conectar con el servidor…" | CA-02.3 |
| 10 | Revocar el permiso desde el administrador de extensiones y tocar el ícono | Mensaje "…no tiene permiso…" y botón **Abrir opciones** | CA-02.3 |
| 11 | Tocar el ícono estando en una URL corta del propio servicio | Mensaje de error del backend, sin crear enlace | RN-09 |
| 12 | Repetir 5 en el otro navegador | Mismo comportamiento con el mismo código | CA-02.4 |

## Notas

- **No se usa CORS**: el acceso al backend se habilita con `optional_host_permissions` y `permissions.request()` ([ADR-0014](../docs/adr/0014-extension-mv3-y-permisos-de-host.md)).
- `permissions.request()` se llama directamente en el manejador del envío del formulario (clic en **Guardar** o Enter), antes de cualquier `await`.
- El QR lo genera el backend (D8, [ADR-0023](../docs/adr/0023-qr-generado-en-el-servidor.md)); la extensión solo muestra la imagen. Si el docente exige generarlo en el cliente, habrá que incorporar una librería JS.
- Requiere Firefox 128 o superior por `optional_host_permissions`.
