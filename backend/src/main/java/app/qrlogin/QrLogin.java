package app.qrlogin;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pedido de login con QR: la computadora lo crea, el celular lo autoriza o rechaza y la computadora
 * lo canjea una sola vez por una sesión (ADR-0033). Los cambios de estado se hacen con UPDATE condicionales
 * en {@link QrLoginRepository}, no sobre esta entidad.
 */
@Entity
@Table(name = "qr_login")
public class QrLogin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "code", nullable = false, unique = true, length = 64)
	private String codigo;

	@Column(name = "secret", nullable = false, length = 64)
	private String secreto;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 12)
	private EstadoQrLogin estado;

	@Column(name = "user_id")
	private Long usuarioId;

	@Column(name = "created_at", nullable = false)
	private Instant creadoEn;

	@Column(name = "expires_at", nullable = false)
	private Instant venceEn;

	protected QrLogin() {
		// requerido por JPA
	}

	public QrLogin(String codigo, String secreto, Instant creadoEn, Instant venceEn) {
		this.codigo = codigo;
		this.secreto = secreto;
		this.estado = EstadoQrLogin.PENDING;
		this.creadoEn = creadoEn;
		this.venceEn = venceEn;
	}

	/** Un pedido está vencido a partir del instante exacto de su vencimiento. */
	public boolean estaVencido(Instant ahora) {
		return !ahora.isBefore(venceEn);
	}

	public Long getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getSecreto() {
		return secreto;
	}

	public EstadoQrLogin getEstado() {
		return estado;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

	public Instant getVenceEn() {
		return venceEn;
	}
}
