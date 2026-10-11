package app.enlace;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import app.common.config.AppProperties;
import app.enlace.alias.GeneradorAlias;

/**
 * Reglas de negocio de los enlaces cortos.
 * Accede a la persistencia con EntityManager y JPQL, sin capa Repository (ADR-0004).
 */
@Service
public class EnlaceService {

	private static final String RESTRICCION_ALIAS = "uk_link_alias";

	private final EntityManager entityManager;
	private final TransactionTemplate transaction;
	private final GeneradorAlias generadorAlias;
	private final ValidadorUrl validadorUrl;
	private final AppProperties propiedades;
	private final Clock clock;

	public EnlaceService(EntityManager entityManager, PlatformTransactionManager transactionManager,
			GeneradorAlias generadorAlias, ValidadorUrl validadorUrl, AppProperties propiedades, Clock clock) {
		this.entityManager = entityManager;
		this.transaction = new TransactionTemplate(transactionManager);
		this.generadorAlias = generadorAlias;
		this.validadorUrl = validadorUrl;
		this.propiedades = propiedades;
		this.clock = clock;
	}

	/**
	 * Crea un enlace corto para la URL indicada. Cada llamada genera un alias nuevo (ADR-0017).
	 * <p>
	 * Cada intento corre en su propia transacción: si otro pedido simultáneo toma el mismo alias,
	 * la restricción UNIQUE hace fallar solo ese intento y se prueba con otro alias.
	 *
	 * @throws UrlInvalidaException       si la URL no es válida (ADR-0019, ADR-0020)
	 * @throws AliasNoDisponibleException si no se encuentra un alias libre tras el máximo de intentos
	 */
	public Enlace crear(String urlOriginal) {
		validadorUrl.validar(urlOriginal);
		int maxIntentos = propiedades.alias().maxIntentos();
		for (int intento = 0; intento < maxIntentos; intento++) {
			String alias = generadorAlias.generar();
			try {
				Optional<Enlace> creado = transaction.execute(status -> intentarAsignar(alias, urlOriginal));
				if (creado.isPresent()) {
					return creado.get();
				}
			} catch (PersistenceException | DataIntegrityViolationException e) {
				if (!esColisionDeAlias(e)) {
					throw e;
				}
				// carrera con otro pedido por el mismo alias: se reintenta con otro
			}
		}
		throw new AliasNoDisponibleException(maxIntentos);
	}

	/** Solo se reintenta la violación UNIQUE del alias; otros errores conservan su causa. */
	private static boolean esColisionDeAlias(Throwable error) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacion) {
				String restriccion = violacion.getConstraintName();
				// HSQLDB puede informar el nombre sin esquema o con el esquema PUBLIC.
				return "23505".equals(violacion.getSQLState())
						&& (RESTRICCION_ALIAS.equalsIgnoreCase(restriccion)
								|| ("PUBLIC." + RESTRICCION_ALIAS).equalsIgnoreCase(restriccion));
			}
		}
		return false;
	}

	/**
	 * Devuelve el enlace vigente asociado al alias.
	 *
	 * @throws EnlaceNoEncontradoException si el alias no existe o el enlace está vencido (no se distinguen, ADR-0018)
	 */
	@Transactional(readOnly = true)
	public Enlace resolver(String alias) {
		Instant ahora = clock.instant();
		return buscarPorAlias(alias)
				.filter(enlace -> !enlace.estaVencido(ahora))
				.orElseThrow(() -> new EnlaceNoEncontradoException(alias));
	}

	/** URL corta pública de un enlace: {@code {app.base-url}/{alias}} (ADR-0022). */
	public String urlCortaDe(Enlace enlace) {
		return propiedades.urlBase().replaceAll("/+$", "") + "/" + enlace.getAlias();
	}

	/**
	 * Intenta asignar el alias: lo crea si no existe, o lo reasigna si está vencido (ADR-0021).
	 * Devuelve vacío si el alias pertenece a un enlace vigente.
	 */
	private Optional<Enlace> intentarAsignar(String alias, String urlOriginal) {
		// la base guarda microsegundos: se trunca para que lo devuelto coincida con lo persistido
		Instant ahora = clock.instant().truncatedTo(ChronoUnit.MICROS);
		Instant venceEn = ahora.plus(propiedades.enlace().ttl());

		Optional<Enlace> existente = buscarPorAlias(alias);
		if (existente.isEmpty()) {
			Enlace enlace = new Enlace(alias, urlOriginal, ahora, venceEn);
			entityManager.persist(enlace);
			entityManager.flush();
			return Optional.of(enlace);
		}
		if (reasignarSiEstaVencido(alias, urlOriginal, ahora, venceEn)) {
			Enlace enlace = existente.get();
			entityManager.refresh(enlace);
			return Optional.of(enlace);
		}
		return Optional.empty();
	}

	private Optional<Enlace> buscarPorAlias(String alias) {
		return entityManager.createQuery("SELECT l FROM Enlace l WHERE l.alias = :alias", Enlace.class)
				.setParameter("alias", alias)
				.getResultStream()
				.findFirst();
	}

	/**
	 * Actualización atómica: solo reasigna si el enlace sigue vencido al momento de actualizar,
	 * así dos pedidos simultáneos no pueden quedarse con el mismo alias.
	 */
	private boolean reasignarSiEstaVencido(String alias, String urlOriginal, Instant ahora, Instant venceEn) {
		int actualizados = entityManager.createQuery("""
				UPDATE Enlace l
				   SET l.urlOriginal = :urlOriginal, l.creadoEn = :ahora, l.venceEn = :venceEn
				 WHERE l.alias = :alias AND l.venceEn <= :ahora
				""")
				.setParameter("urlOriginal", urlOriginal)
				.setParameter("ahora", ahora)
				.setParameter("venceEn", venceEn)
				.setParameter("alias", alias)
				.executeUpdate();
		return actualizados == 1;
	}
}
