package app.espacio;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import app.usuario.Usuario;

/**
 * Espacio de nombres de un usuario: el primer segmento de {@code /melina/1} (ADR-0028).
 * Un usuario tiene un único espacio y el nombre es único; ambas reglas las garantiza la base.
 */
@Entity
@Table(name = "namespace")
public class Espacio {

	/** Límite técnico del equipo, igual al tamaño de la columna (ADR-0029). */
	public static final int LARGO_MAXIMO = 15;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, unique = true, length = 15)
	private String nombre;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private Usuario usuario;

	protected Espacio() {
		// requerido por JPA
	}

	public Espacio(String nombre, Usuario usuario) {
		this.nombre = normalizar(nombre);
		this.usuario = usuario;
	}

	/** Cambia el nombre; el anterior queda libre para otros usuarios (ADR-0031). */
	public void renombrar(String nuevoNombre) {
		this.nombre = normalizar(nuevoNombre);
	}

	/** Los espacios se tratan siempre en minúsculas: {@code Melina}, {@code MELINA} y {@code melina} son el mismo. */
	public static String normalizar(String nombre) {
		return nombre.toLowerCase(Locale.ROOT);
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Usuario getUsuario() {
		return usuario;
	}
}
