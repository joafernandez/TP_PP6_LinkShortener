# Link Shortener – TP Integrador Paradigmas de Programación VI

Servicio de acortamiento y gestión de enlaces. La consigna está en [docs/consigna.md](docs/consigna.md).

## Estructura

| Carpeta | Contenido |
|---|---|
| `backend/` | API REST en Java con Spring Boot, JPA/Hibernate y HSQLDB |
| `frontend/` | Página web en HTML, CSS y JavaScript, incorporada al backend al compilar |
| `extension/` | Extensión para Chrome y Firefox (prevista para el incremento 7) |
| `database/` | Archivos de la base HSQLDB local (no se versionan) |
| `docs/` | Consigna, minutas, requerimientos, decisiones de diseño y contrato de la API |

## Requisitos

- JDK 25
- No hace falta instalar Gradle: se usa el Gradle Wrapper incluido (`gradlew`).

## Backend

Desde la carpeta `backend/`:

```bash
./gradlew test       # ejecuta los tests y genera el reporte de cobertura
./gradlew bootRun    # levanta la aplicación en http://localhost:8080
```

En Windows usar `gradlew.bat` en lugar de `./gradlew`.

- Reporte de cobertura (JaCoCo): `backend/build/reports/jacoco/test/html/index.html`
- La base de datos se guarda en `database/`. Para usar otra ubicación, definir la variable de entorno `APP_DB_PATH`.

## Usar la página web (incremento 6)

Desde `backend/`, en PowerShell:

```powershell
$env:APP_BASE_URL = 'http://localhost:8081'
.\gradlew.bat bootRun --args='--server.port=8081'
```

Se requiere JDK 25; la configuración de la copia portable de este equipo está en [CLAUDE.md](CLAUDE.md), sección «Entorno local».

Abrir `http://localhost:8081/`, ingresar una dirección completa y presionar **ACORTAR** o Enter. La página muestra la URL corta, su vencimiento en hora local y el QR. Los errores del backend y de conexión aparecen en la pantalla; si falla el QR, se conserva el enlace creado.

Los archivos fuente están en `frontend/`. Gradle los copia a `backend/build/resources/main/static/` y los incluye en el JAR; editar siempre las fuentes y volver a ejecutar `bootRun` después de detener el proceso anterior para incorporar cambios. La web consulta rutas relativas, por lo que usa el mismo servidor que la sirve. Si se cambia el puerto, ajustar también `APP_BASE_URL` para que los enlaces publicados apunten al lugar correcto.

Para compartir con otro dispositivo, configurar `APP_BASE_URL` con una dirección del servidor accesible desde ese dispositivo. `localhost` es apropiado para las pruebas en la misma computadora.

## Probar el código QR (incremento 5)

El QR se obtiene con `GET /api/v1/links/{alias}/qr`. Devuelve un PNG de 300 × 300 píxeles que codifica la **URL corta**. No crea un enlace nuevo ni renueva su vencimiento.

1. Levantar el backend. Si se usa el puerto 8081, configurar también la URL base. Desde `backend/`, en PowerShell:

   ```powershell
   $env:APP_BASE_URL = 'http://localhost:8081'
   .\gradlew.bat bootRun --args='--server.port=8081'
   ```

   Se requiere JDK 25. La configuración de la copia portable preparada en este equipo está documentada en [CLAUDE.md](CLAUDE.md), sección «Entorno local».

2. Crear un enlace desde Swagger UI (`http://localhost:8081/swagger-ui.html`) o desde otra consola PowerShell:

   ```powershell
   $qrExampleLink = Invoke-RestMethod -Uri 'http://localhost:8081/api/v1/links' `
       -Method Post -ContentType 'application/json' `
       -Body '{"url":"https://example.org"}'
   $qrExampleLink.shortUrl
   ```

3. Abrir `http://localhost:8081/api/v1/links/{alias}/qr` en el navegador, reemplazando `{alias}` por el valor devuelto. También se puede guardar la imagen:

   ```powershell
   Invoke-WebRequest -Uri ('http://localhost:8081/api/v1/links/{0}/qr' -f $qrExampleLink.alias) `
       -OutFile '.\build\qr.png'
   ```

4. El QR debe contener exactamente `shortUrl`. Al abrir esa dirección, el enlace vigente redirige a la URL original.

Si el enlace venció o el alias no existe, el endpoint del QR devuelve `404` con `application/problem+json`. Tanto el PNG como ese error incluyen `Cache-Control: no-store` para comprobar nuevamente la vigencia en pedidos posteriores. La URL corta pública sigue mostrando su página HTML 404 cuando corresponde.
