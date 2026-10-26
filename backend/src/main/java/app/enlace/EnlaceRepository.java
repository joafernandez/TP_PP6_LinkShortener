package app.enlace;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Persistencia de los enlaces con EntityManager y JPQL (ADR-0024). Participa en las transacciones del service. */
@Repository
public class EnlaceRepository {

	private static final String RESTRICCION_ALIAS = "uk_link_alias";

	private final EntityManager entityManager;

	public EnlaceRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Optional<Enlace> buscarPorAlias(String alias) {
		return entityManager.createQuery("SELECT l FROM Enlace l WHERE l.alias = :alias", Enlace.class)
				.setParameter("alias", alias)
				.getResultStream()
				.findFirst();
	}

	/** Alias de los enlaces vigentes: los que el generador no puede asignar (ADR-0027). */
	public Set<String> buscarAliasVigentes(Instant ahora) {
		return entityManager.createQuery("SELECT l.alias FROM Enlace l WHERE l.venceEn > :ahora", String.class)
				.setParameter("ahora", ahora)
				.getResultStream()
				.collect(Collectors.toSet());
	}

	/**
	 * Inserta y sincroniza para detectar la colisión antes de terminar el intento.
	 * IDENTITY puede ejecutar la inserción ya en persist, por eso ambas operaciones están dentro del try.
	 */
	public void insertar(Enlace enlace) {
		try {
			entityManager.persist(enlace);
			entityManager.flush();
		} catch (PersistenceException | DataIntegrityViolationException e) {
			if (esColisionDeAlias(e)) {
				throw new ColisionAliasException(e);
			}
			throw e;
		}
	}

	/**
	 * Actualiza solo si el registro sigue vencido al ejecutar el UPDATE. Si tuvo éxito, refresca
	 * la entidad administrada para devolver las mismas fechas y URL que quedaron en la base.
	 */
	public boolean reasignarSiEstaVencido(Enlace enlace, String urlOriginal, Instant ahora, Instant venceEn) {
		int actualizados = entityManager.createQuery("""
				UPDATE Enlace l
				   SET l.urlOriginal = :urlOriginal, l.creadoEn = :ahora, l.venceEn = :venceEn
				 WHERE l.alias = :alias AND l.venceEn <= :ahora
				""")
				.setParameter("urlOriginal", urlOriginal)
				.setParameter("ahora", ahora)
				.setParameter("venceEn", venceEn)
				.setParameter("alias", enlace.getAlias())
				.executeUpdate();
		if (actualizados == 1) {
			entityManager.refresh(enlace);
			return true;
		}
		return false;
	}

	/** Solo la restricción UNIQUE del alias permite reintentar; otros errores conservan su causa. */
	private static boolean esColisionDeAlias(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacion) {
				String restriccion = violacion.getConstraintName();
				return "23505".equals(violacion.getSQLState())
						&& (RESTRICCION_ALIAS.equalsIgnoreCase(restriccion)
								|| ("PUBLIC." + RESTRICCION_ALIAS).equalsIgnoreCase(restriccion));
			}
		}
		return false;
	}
}
