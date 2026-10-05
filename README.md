# Link Shortener – TP Integrador Paradigmas de Programación VI

Servicio de acortamiento y gestión de enlaces. La consigna está en [docs/consigna.md](docs/consigna.md).

## Estructura

| Carpeta | Contenido |
|---|---|
| `backend/` | API REST en Java con Spring Boot, JPA/Hibernate y HSQLDB |
| `frontend/` | Página web (cliente de la API) |
| `extension/` | Extensión para Chrome y Firefox (cliente de la API) |
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
