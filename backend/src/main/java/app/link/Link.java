package app.link;

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
public class Link {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 16)
	private String alias;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String originalUrl;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected Link() {
		// requerido por JPA
	}

	public Link(String alias, String originalUrl, Instant createdAt, Instant expiresAt) {
		this.alias = alias;
		this.originalUrl = originalUrl;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	/** Un enlace está vencido a partir del instante exacto de su vencimiento. */
	public boolean isExpired(Instant now) {
		return !now.isBefore(expiresAt);
	}

	public Long getId() {
		return id;
	}

	public String getAlias() {
		return alias;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
