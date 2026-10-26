package app.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuario autenticado con Google (ADR-0028). Se identifica por el {@code sub} de Google,
 * que no cambia aunque el usuario cambie su email.
 */
@Entity
@Table(name = "app_user")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "google_sub", nullable = false, unique = true, length = 255)
	private String googleSub;

	@Column(length = 320)
	private String email;

	@Column(name = "name", length = 255)
	private String nombre;

	@Column(name = "created_at", nullable = false)
	private Instant creadoEn;

	protected Usuario() {
		// requerido por JPA
	}

	public Usuario(String googleSub, String email, String nombre, Instant creadoEn) {
		this.googleSub = googleSub;
		this.email = email;
		this.nombre = nombre;
		this.creadoEn = creadoEn;
	}

	public Long getId() {
		return id;
	}

	public String getGoogleSub() {
		return googleSub;
	}

	public String getEmail() {
		return email;
	}

	public String getNombre() {
		return nombre;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}
}
