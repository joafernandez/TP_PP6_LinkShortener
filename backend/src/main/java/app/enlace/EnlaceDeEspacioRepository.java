package app.enlace;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import app.common.persistencia.RestriccionUnica;

/**
 * Persistencia de los enlaces de espacio con EntityManager y JPQL (ADR-0024 y ADR-0032).
 * Participa en las transacciones del service, igual que {@link EnlaceRepository}.
 */
@Repository
public class EnlaceDeEspacioRepository {

	private static final String RESTRICCION_ALIAS = "uk_namespace_link_alias";

	private final EntityManager entityManager;

	public EnlaceDeEspacioRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Optional<EnlaceDeEspacio> buscar(Long espacioId, String alias) {
		return entityManager.createQuery("""
				SELECT l FROM EnlaceDeEspacio l WHERE l.espacio.id = :espacioId AND l.alias = :alias
				""", EnlaceDeEspacio.class)
				.setParameter("espacioId", espacioId)
				.setParameter("alias", alias)
				.getResultStream()
				.findFirst();
	}

	/** Busca por el nombre público del espacio, como llega en la URL corta. */
	public Optional<EnlaceDeEspacio> buscarPorNombreDeEspacio(String nombreEspacio, String alias) {
		return entityManager.createQuery("""
				SELECT l FROM EnlaceDeEspacio l WHERE l.espacio.nombre = :espacio AND l.alias = :alias
				""", EnlaceDeEspacio.class)
				.setParameter("espacio", nombreEspacio)
				.setParameter("alias", alias)
				.getResultStream()
				.findFirst();
	}

	/** Códigos vigentes del espacio: los que el generador no puede asignar (ADR-0027). */
	public Set<String> buscarAliasVigentes(Long espacioId, Instant ahora) {
		return entityManager.createQuery("""
				SELECT l.alias FROM EnlaceDeEspacio l WHERE l.espacio.id = :espacioId AND l.venceEn > :ahora
				""", String.class)
				.setParameter("espacioId", espacioId)
				.setParameter("ahora", ahora)
				.getResultStream()
				.collect(Collectors.toSet());
	}

	/** Inserta y sincroniza; un código repetido en el espacio se traduce a {@link ColisionAliasException}. */
	public void insertar(EnlaceDeEspacio enlace) {
		try {
			entityManager.persist(enlace);
			entityManager.flush();
		} catch (PersistenceException | DataIntegrityViolationException e) {
			if (RestriccionUnica.esViolacion(e, RESTRICCION_ALIAS)) {
				throw new ColisionAliasException(e);
			}
			throw e;
		}
	}

	/** Igual que {@link EnlaceRepository#reasignarSiEstaVencido}, dentro del espacio del enlace. */
	public boolean reasignarSiEstaVencido(EnlaceDeEspacio enlace, String urlOriginal, Instant ahora, Instant venceEn) {
		int actualizados = entityManager.createQuery("""
				UPDATE EnlaceDeEspacio l
				   SET l.urlOriginal = :urlOriginal, l.creadoEn = :ahora, l.venceEn = :venceEn
				 WHERE l.id = :id AND l.venceEn <= :ahora
				""")
				.setParameter("urlOriginal", urlOriginal)
				.setParameter("ahora", ahora)
				.setParameter("venceEn", venceEn)
				.setParameter("id", enlace.getId())
				.executeUpdate();
		if (actualizados == 1) {
			entityManager.refresh(enlace);
			return true;
		}
		return false;
	}

	/** Vence ya los enlaces vigentes del espacio: dejan de redirigir y sus códigos quedan libres (ADR-0031). */
	public int vencerVigentes(Long espacioId, Instant ahora) {
		return entityManager.createQuery("""
				UPDATE EnlaceDeEspacio l SET l.venceEn = :ahora WHERE l.espacio.id = :espacioId AND l.venceEn > :ahora
				""")
				.setParameter("ahora", ahora)
				.setParameter("espacioId", espacioId)
				.executeUpdate();
	}
}
