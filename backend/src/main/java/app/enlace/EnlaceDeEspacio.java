package app.enlace;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import app.espacio.Espacio;

/**
 * Enlace corto dentro del espacio de un usuario: {@code /{espacio}/{código}} (ADR-0032).
 * El código es único solo dentro del espacio; al reasignarse un código vencido se actualiza este registro.
 */
@Entity
@Table(name = "namespace_link")
public class EnlaceDeEspacio {

	/** Límite técnico del equipo para el código personalizado, igual al tamaño de la columna (ADR-0029). */
	public static final int LARGO_MAXIMO_CODIGO = 15;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// carga inmediata: la URL corta necesita el nombre del espacio fuera de la transacción
	@ManyToOne(optional = false)
	@JoinColumn(name = "namespace_id", nullable = false)
	private Espacio espacio;

	@Column(nullable = false, length = LARGO_MAXIMO_CODIGO)
	private String alias;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String urlOriginal;

	@Column(name = "created_at", nullable = false)
	private Instant creadoEn;

	@Column(name = "expires_at", nullable = false)
	private Instant venceEn;

	protected EnlaceDeEspacio() {
		// requerido por JPA
	}

	public EnlaceDeEspacio(Espacio espacio, String alias, String urlOriginal, Instant creadoEn, Instant venceEn) {
		this.espacio = espacio;
		this.alias = alias;
		this.urlOriginal = urlOriginal;
		this.creadoEn = creadoEn;
		this.venceEn = venceEn;
	}

	/** Igual que {@link Enlace#estaVencido}: vencido a partir del instante exacto de su vencimiento. */
	public boolean estaVencido(Instant ahora) {
		return !ahora.isBefore(venceEn);
	}

	public Long getId() {
		return id;
	}

	public Espacio getEspacio() {
		return espacio;
	}

	public String getAlias() {
		return alias;
	}

	public String getUrlOriginal() {
		return urlOriginal;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

	public Instant getVenceEn() {
		return venceEn;
	}
}
