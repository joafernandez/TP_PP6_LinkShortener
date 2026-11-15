package app.espacio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import app.common.persistencia.RestriccionUnica;

/**
 * Persistencia de los nombres de espacio con EntityManager y JPQL (ADR-0024 y ADR-0034).
 * Participa en las transacciones del service.
 */
@Repository
public class NombreDeEspacioRepository {

	private static final String RESTRICCION_NOMBRE = "uk_namespace_name_value";

	private final EntityManager entityManager;

	public NombreDeEspacioRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/** El nombre, vigente o no. */
	public Optional<NombreDeEspacio> buscar(String nombre) {
		return entityManager.createQuery("SELECT n FROM NombreDeEspacio n WHERE n.nombre = :nombre", NombreDeEspacio.class)
				.setParameter("nombre", Espacio.normalizar(nombre))
				.getResultStream()
				.findFirst();
	}

	/** El espacio al que lleva un nombre principal, o uno anterior todavía en transición. */
	public Optional<Espacio> buscarEspacioVigente(String nombre, Instant ahora) {
		return entityManager.createQuery("""
				SELECT n.espacio FROM NombreDeEspacio n
				 WHERE n.nombre = :nombre AND (n.venceEn IS NULL OR n.venceEn > :ahora)
				""", Espacio.class)
				.setParameter("nombre", Espacio.normalizar(nombre))
				.setParameter("ahora", ahora)
				.getResultStream()
				.findFirst();
	}

	/** Nombres anteriores del espacio que siguen en transición, del que vence antes al que vence después. */
	public List<NombreDeEspacio> buscarAnterioresVigentes(Long espacioId, Instant ahora) {
		return entityManager.createQuery("""
				SELECT n FROM NombreDeEspacio n
				 WHERE n.espacio.id = :espacioId AND n.venceEn > :ahora
				 ORDER BY n.venceEn, n.nombre
				""", NombreDeEspacio.class)
				.setParameter("espacioId", espacioId)
				.setParameter("ahora", ahora)
				.getResultList();
	}

	/** Inserta y sincroniza; un nombre ya registrado se traduce a {@link EspacioOcupadoException}. */
	public void insertar(NombreDeEspacio nombre) {
		guardar(nombre.getNombre(), () -> entityManager.persist(nombre));
	}

	/** Sincroniza los cambios de los nombres administrados (principal o transición). */
	public void guardarCambios(String nombre) {
		guardar(nombre, () -> { });
	}

	/**
	 * Pasa a {@code espacio}, como principal, un nombre cuya transición ya venció. El {@code UPDATE} condicional
	 * hace que, de dos pedidos simultáneos, gane uno solo (igual que ADR-0021). Si tuvo éxito, refresca la entidad.
	 */
	public boolean reasignarSiEstaVencido(NombreDeEspacio nombre, Espacio espacio, Instant ahora) {
		int actualizados = entityManager.createQuery("""
				UPDATE NombreDeEspacio n SET n.espacio = :espacio, n.venceEn = NULL
				 WHERE n.id = :id AND n.venceEn <= :ahora
				""")
				.setParameter("espacio", espacio)
				.setParameter("id", nombre.getId())
				.setParameter("ahora", ahora)
				.executeUpdate();
		if (actualizados == 1) {
			entityManager.refresh(nombre);
			return true;
		}
		return false;
	}

	private void guardar(String nombre, Runnable operacion) {
		try {
			operacion.run();
			entityManager.flush();
		} catch (PersistenceException | DataIntegrityViolationException e) {
			if (RestriccionUnica.esViolacion(e, RESTRICCION_NOMBRE)) {
				throw new EspacioOcupadoException(nombre, e);
			}
			throw e;
		}
	}
}
