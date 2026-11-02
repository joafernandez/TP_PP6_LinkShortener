package app.qrlogin;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

/**
 * Persistencia de los pedidos de login con QR con EntityManager y JPQL (ADR-0024).
 * Las transiciones de estado son UPDATE condicionales: si dos pedidos compiten por el mismo QR,
 * la base deja pasar solo a uno (el que ve el estado esperado y la vigencia al ejecutar el UPDATE).
 */
@Repository
public class QrLoginRepository {

	private final EntityManager entityManager;

	public QrLoginRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void insertar(QrLogin qrLogin) {
		entityManager.persist(qrLogin);
		entityManager.flush();
	}

	public Optional<QrLogin> buscarPorCodigo(String codigo) {
		return entityManager.createQuery("SELECT q FROM QrLogin q WHERE q.codigo = :codigo", QrLogin.class)
				.setParameter("codigo", codigo)
				.getResultStream()
				.findFirst();
	}

	/** Usuario que autorizó el pedido. Lee la base, no una entidad ya cargada. */
	public Optional<Long> buscarUsuarioId(String codigo) {
		return entityManager.createQuery("SELECT q.usuarioId FROM QrLogin q WHERE q.codigo = :codigo", Long.class)
				.setParameter("codigo", codigo)
				.getResultStream()
				.filter(id -> id != null)
				.findFirst();
	}

	/** Pasa de pendiente a {@code nuevoEstado} solo si sigue pendiente y vigente. Devuelve 1 si lo logró, 0 si no. */
	public int responderSiPendiente(String codigo, EstadoQrLogin nuevoEstado, Long usuarioId, Instant ahora) {
		return entityManager.createQuery("""
				UPDATE QrLogin q
				   SET q.estado = :nuevo, q.usuarioId = :usuarioId
				 WHERE q.codigo = :codigo AND q.estado = :pendiente AND q.venceEn > :ahora
				""")
				.setParameter("nuevo", nuevoEstado)
				.setParameter("usuarioId", usuarioId)
				.setParameter("codigo", codigo)
				.setParameter("pendiente", EstadoQrLogin.PENDING)
				.setParameter("ahora", ahora)
				.executeUpdate();
	}

	/** Canjea el pedido: pasa de autorizado a usado solo si sigue autorizado y vigente. Es lo que lo vuelve de un solo uso. */
	public int consumirSiAutorizado(String codigo, Instant ahora) {
		return entityManager.createQuery("""
				UPDATE QrLogin q
				   SET q.estado = :usado
				 WHERE q.codigo = :codigo AND q.estado = :autorizado AND q.venceEn > :ahora
				""")
				.setParameter("usado", EstadoQrLogin.USED)
				.setParameter("codigo", codigo)
				.setParameter("autorizado", EstadoQrLogin.AUTHORIZED)
				.setParameter("ahora", ahora)
				.executeUpdate();
	}

	/** Borra los pedidos que vencieron antes del instante indicado. */
	public int eliminarVencidosAntesDe(Instant limite) {
		return entityManager.createQuery("DELETE FROM QrLogin q WHERE q.venceEn < :limite")
				.setParameter("limite", limite)
				.executeUpdate();
	}
}
