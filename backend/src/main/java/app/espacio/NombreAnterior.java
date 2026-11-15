package app.espacio;

import java.time.Instant;

/** Un nombre anterior del espacio que sigue funcionando hasta {@code venceEn} (ADR-0034). */
public record NombreAnterior(String nombre, Instant venceEn) {
}
