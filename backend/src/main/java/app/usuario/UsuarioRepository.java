package app.usuario;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

/** Persistencia de los usuarios con EntityManager y JPQL (ADR-0024). Participa en las transacciones del service. */
@Repository
public class UsuarioRepository {

	private final EntityManager entityManager;

	public UsuarioRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Optional<Usuario> buscarPorGoogleSub(String googleSub) {
		return entityManager.createQuery("SELECT u FROM Usuario u WHERE u.googleSub = :googleSub", Usuario.class)
				.setParameter("googleSub", googleSub)
				.getResultStream()
				.findFirst();
	}

	public Optional<Usuario> buscarPorId(Long id) {
		return Optional.ofNullable(entityManager.find(Usuario.class, id));
	}

	/** Inserta y sincroniza: un {@code sub} repetido falla aquí por la restricción UNIQUE. */
	public void insertar(Usuario usuario) {
		entityManager.persist(usuario);
		entityManager.flush();
	}
}
