package app.enlace;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Enlace corto: asocia un alias a una URL original durante un período de vigencia.
 * Un único registro por alias; al reasignarse un alias vencido se actualiza este registro (ADR-0021).
 */
@Entity
@Table(name = "link")
public class Enlace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 16)
	private String alias;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String urlOriginal;

	@Column(name = "created_at", nullable = false)
	private Instant creadoEn;

	@Column(name = "expires_at", nullable = false)
	private Instant venceEn;

	protected Enlace() {
		// requerido por JPA
	}

	public Enlace(String alias, String urlOriginal, Instant creadoEn, Instant venceEn) {
		this.alias = alias;
		this.urlOriginal = urlOriginal;
		this.creadoEn = creadoEn;
		this.venceEn = venceEn;
	}

	/** Un enlace está vencido a partir del instante exacto de su vencimiento. */
	public boolean estaVencido(Instant ahora) {
		return !ahora.isBefore(venceEn);
	}

	public Long getId() {
		return id;
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
