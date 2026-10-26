# 003 – Paso 2.1: usuarios y espacios en la base de datos

- **Fecha:** 2026-10-25
- **Herramienta:** Claude Code (VS Code)
- **Objetivo:** preparar la base de datos para los usuarios autenticados con Google y su espacio de nombres, con la unicidad garantizada por la base. Primer paso de la parte B de la Etapa 2.
- **Estado:** implementado y verificado. Sin commit.

## Prompt (resumido)

El equipo aprobó agregar Spring Security para el login con Google, y estos criterios: con sesión, los enlaces van siempre al espacio del usuario; un único espacio por usuario y único entre usuarios; normalización a minúsculas (`Melina` = `melina`); el espacio con minúsculas y números; el código personalizado con minúsculas, números, `-` y `_`; un código ocupado se rechaza con un mensaje claro; los códigos pueden repetirse entre usuarios.

Sobre los largos, el equipo aclaró que **el docente no los definió** y pidió no convertirlos en requisitos funcionales: si hacen falta límites técnicos, documentarlos como decisión del equipo. Pidió ver la lista exacta de archivos antes de modificar nada. Tras revisarla, la aprobó con un ajuste: **15 caracteres como máximo** para el espacio y para el código personalizado, en lugar de los 30 y 64 propuestos.

## Diseño antes del código

- Tablas `app_user` (`user` es palabra reservada) y `namespace`, en la migración `V2`. La tabla `link` no se toca: los enlaces anónimos siguen igual.
- Restricciones en la base: `UNIQUE (name)`, `UNIQUE (user_id)` y un `CHECK` con `REGEXP_MATCHES(name, '[a-z0-9]+')`. La columna `name` es `VARCHAR(15)`.
- Paquetes hermanos `app.usuario` y `app.espacio`. Los Repository siguen ADR-0024: `EntityManager`, JPQL con parámetros nombrados, participan en las transacciones del service.
- `Espacio` normaliza a minúsculas al construirse; `EspacioRepository.buscarPorNombre` normaliza al buscar.
- `EspacioRepository.insertar` traduce **solo** `uk_namespace_name` a `EspacioOcupadoException`. Un segundo espacio para el mismo usuario conserva el error original, porque el service del paso 2.3 lo debe evitar antes.
- Sin dependencias nuevas: Spring Security se agrega en el paso 2.2, cuando se usa.

## Cambios

| Tipo | Archivos |
|---|---|
| Base de datos | `db/migration/V2__create_user_and_namespace.sql` |
| Código | `app/usuario/Usuario.java`, `UsuarioRepository.java`; `app/espacio/Espacio.java`, `EspacioRepository.java`, `EspacioOcupadoException.java` |
| Tests | `UsuarioRepositoryIT` (3), `EspacioRepositoryIT` (6), `EspacioTest` (6) |
| Documentación | ADR-0028 (modelo), ADR-0029 (límites técnicos, marcados como decisión del equipo), minuta 02 (criterios aprobados), índice de ADRs, `CLAUDE.md` |

## Verificación

- Primera compilación: falló un test por un `assertThat` ambiguo sobre el resultado de `TransactionTemplate.execute`. Se corrigió declarando el tipo `Optional<Usuario>`.
- `gradlew test --offline --no-daemon`: **147 tests**, sin fallos ni omitidos. JaCoCo: **96,66 % de instrucciones**, **96,63 % de líneas**, **90,62 % de ramas**, por encima del umbral de ADR-0025.
- Los tests comprueban en la base real (HSQLDB en memoria): `Melina` y `MELINA` chocan; un usuario no puede tener dos espacios; el `CHECK` rechaza mayúsculas, espacios, `-`, `_` y `!` aunque se inserte con SQL directo; se aceptan 15 caracteres y se rechazan 16.
- Servidor real con la base `database/`: Flyway aplicó la `V2` («now at version v2») y el acortado anónimo siguió funcionando (`/1`).

## Pendiente

- Paso 2.2: login con Google. Hace falta crear el cliente OAuth en Google Cloud Console.
