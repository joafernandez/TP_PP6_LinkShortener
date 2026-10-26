package app.espacio;

import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Persistencia de los espacios con EntityManager y JPQL (ADR-0024). Participa en las transacciones del service. */
@Repository
public class EspacioRepository {

	private static final String RESTRICCION_NOMBRE = "uk_namespace_name";

	private final EntityManager entityManager;

	public EspacioRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/** Busca sin distinguir mayúsculas: el nombre se normaliza igual que al guardarlo. */
	public Optional<Espacio> buscarPorNombre(String nombre) {
		return entityManager.createQuery("SELECT e FROM Espacio e WHERE e.nombre = :nombre", Espacio.class)
				.setParameter("nombre", Espacio.normalizar(nombre))
				.getResultStream()
				.findFirst();
	}

	public Optional<Espacio> buscarPorUsuario(Long usuarioId) {
		return entityManager.createQuery("SELECT e FROM Espacio e WHERE e.usuario.id = :usuarioId", Espacio.class)
				.setParameter("usuarioId", usuarioId)
				.getResultStream()
				.findFirst();
	}

	/**
	 * Inserta y sincroniza. Solo un nombre repetido se traduce a {@link EspacioOcupadoException};
	 * otros errores, como un segundo espacio para el mismo usuario, conservan su causa.
	 */
	public void insertar(Espacio espacio) {
		guardar(espacio, () -> entityManager.persist(espacio));
	}

	/** Sincroniza un cambio de nombre de un espacio administrado, con la misma traducción de errores que insertar. */
	public void guardarCambioDeNombre(Espacio espacio) {
		guardar(espacio, () -> { });
	}

	private void guardar(Espacio espacio, Runnable operacion) {
		try {
			operacion.run();
			entityManager.flush();
		} catch (PersistenceException | DataIntegrityViolationException e) {
			if (esNombreRepetido(e)) {
				throw new EspacioOcupadoException(espacio.getNombre(), e);
			}
			throw e;
		}
	}

	private static boolean esNombreRepetido(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacion) {
				String restriccion = violacion.getConstraintName();
				return "23505".equals(violacion.getSQLState())
						&& (RESTRICCION_NOMBRE.equalsIgnoreCase(restriccion)
								|| ("PUBLIC." + RESTRICCION_NOMBRE).equalsIgnoreCase(restriccion));
			}
		}
		return false;
	}
}
