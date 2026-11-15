package app.espacio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Un nombre de un espacio: el principal (sin vencimiento) o uno anterior en transición (ADR-0034).
 * Mientras está vigente, redirige a los enlaces del espacio y nadie más puede usarlo.
 */
@Entity
@Table(name = "namespace_name")
public class NombreDeEspacio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "namespace_id", nullable = false)
	private Espacio espacio;

	@Column(name = "name", nullable = false, unique = true, length = Espacio.LARGO_MAXIMO)
	private String nombre;

	/** Null en el nombre principal. */
	@Column(name = "expires_at")
	private Instant venceEn;

	protected NombreDeEspacio() {
		// requerido por JPA
	}

	/** Crea el nombre principal de un espacio. */
	public NombreDeEspacio(Espacio espacio, String nombre) {
		this.espacio = espacio;
		this.nombre = Espacio.normalizar(nombre);
	}

	public boolean esPrincipal() {
		return venceEn == null;
	}

	/** El principal siempre está vigente; uno anterior, hasta el instante exacto de su vencimiento. */
	public boolean estaVigente(Instant ahora) {
		return venceEn == null || ahora.isBefore(venceEn);
	}

	/** Recuperar un nombre propio todavía reservado: vuelve a ser el principal. */
	public void hacerPrincipal() {
		this.venceEn = null;
	}

	/** El nombre deja de ser el principal: sigue funcionando y reservado hasta {@code venceEn}. */
	public void iniciarTransicion(Instant venceEn) {
		this.venceEn = venceEn;
	}

	public Long getId() {
		return id;
	}

	public Espacio getEspacio() {
		return espacio;
	}

	public String getNombre() {
		return nombre;
	}

	public Instant getVenceEn() {
		return venceEn;
	}
}
