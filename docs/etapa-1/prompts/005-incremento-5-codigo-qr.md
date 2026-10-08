# 005 – Incremento 5: código QR

- **Fecha de inicio:** 2026-10-08
- **Herramienta:** Codex
- **Objetivo:** implementar el QR del enlace corto según el contrato OpenAPI y el ADR-0023, con tests y documentación, explicando el código por pasos a una integrante con experiencia en PHP.
- **Estado:** en curso. Paso 1 (servicio y tests unitarios) implementado y verificado. El incremento 4 ya está terminado; el incremento 6 comenzará después del QR.

## Pedidos del equipo

1. Planificar los incrementos 5 (QR) y 6 (página web), manteniendo el proceso de código, pruebas y documentación usado en los incrementos anteriores.
2. Preparar la propuesta concreta del incremento 5, con responsabilidades, archivos afectados y criterios de verificación.
3. Aprobar la implementación: «dale pero anda implementando de a poco y me vas explicando asi entiendo, no se nada de java pero como sabes soy muy buena en php».

## Diseño antes del código

- QR generado en el backend con ZXing `core` y `javase` 3.5.4, verificados en Maven Central: https://central.sonatype.com/artifact/com.google.zxing/core/3.5.4 y https://central.sonatype.com/artifact/com.google.zxing/javase/3.5.4.
- PNG de 300 × 300 píxeles, tamaño fijo mediante una constante, generado en memoria y con texto en UTF-8.
- `QrCodeService`: texto → bytes de PNG, sin HTTP ni persistencia.
- `QrController`: `GET /api/v1/links/{alias}/qr`, reutilizando `LinkService.resolve` y `shortUrlOf`.
- Enlace vigente: 200 PNG. Vencido o inexistente: 404 ProblemDetail en JSON. La redirección mantiene su manejador local HTML.
- `Cache-Control: no-store` en el endpoint y sus errores 404 para consultar nuevamente la vigencia en pedidos posteriores.
- Sin cambios de esquema. Se conserva el supuesto D8 del ADR-0023, pendiente de confirmación del Cliente.

## Secuencia de implementación

1. Documentar el diseño y agregar el generador de PNG con sus tests unitarios.
2. Conectar el generador al endpoint y agregar el manejo de 404 JSON y sus tests de integración.
3. Ejecutar la suite completa, comprobar manualmente el endpoint y actualizar los documentos con resultados reales.

## Verificación

### Paso 1: servicio generador de PNG

- Archivos de implementación: `QrCodeService.java`, `QrCodeServiceTest.java` y dependencias ZXing en `build.gradle`.
- Primera ejecución de `gradlew test --tests app.qr.QrCodeServiceTest --no-daemon`: detenida antes de compilar porque faltaba el JDK 25 requerido.
- Tras preparar el JDK portable, el código compiló. De los 3 casos unitarios, el lector automático no detectó el QR del caso con caracteres Unicode (`NotFoundException`).
- La comprobación aislada del mismo PNG mostró que podía leerse en modo `PURE_BARCODE`. Se usa ese modo en el test porque la entrada es una imagen de un único QR, sin fondo ni perspectiva; se conserva la comparación exacta del contenido, incluidos los caracteres Unicode. No se modificó el generador para eludir el caso.
- Verificación final: `gradlew test --no-daemon` con JDK 25 → **BUILD SUCCESSFUL**, **69 tests, 0 fallos y 0 errores** (66 existentes y 3 casos nuevos).
- JaCoCo: **95,87 % de instrucciones** y **94,70 % de líneas** para el backend. `QrCodeService`: **100 % de instrucciones y líneas**.
- El endpoint, sus pruebas de integración y la prueba manual HTTP siguen pendientes. No se considera cerrado el incremento 5.

## Observaciones del entorno

- La consola de esta copia usa JDK 21; el proyecto configura un toolchain JDK 25.
- Se descargó Temurin JDK 25 desde la API oficial de Adoptium y se verificó su SHA256. Es una copia portable en `backend/.gradle/toolchains/jdk-25.0.4.1+1/`, ignorada por git, sin cambiar la instalación ni las variables de entorno permanentes del sistema. Se ubicó fuera de `build/` para que `gradlew clean` no borre el JDK.
- Para repetir la suite en esta copia, desde `backend/`, en una sesión de PowerShell:

```powershell
$env:JAVA_HOME = (Resolve-Path '.\.gradle\toolchains\jdk-25.0.4.1+1').Path
.\gradlew.bat test --no-daemon
```

El JDK portable no se versiona: los demás integrantes necesitan su propio JDK 25.
