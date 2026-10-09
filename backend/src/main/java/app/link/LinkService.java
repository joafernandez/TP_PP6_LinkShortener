package app.link;

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
import app.link.alias.AliasGenerator;

/**
 * Reglas de negocio de los enlaces cortos.
 * Accede a la persistencia con EntityManager y JPQL, sin capa Repository (ADR-0004).
 */
@Service
public class LinkService {

	private static final String ALIAS_CONSTRAINT = "uk_link_alias";

	private final EntityManager entityManager;
	private final TransactionTemplate transaction;
	private final AliasGenerator aliasGenerator;
	private final UrlValidator urlValidator;
	private final AppProperties properties;
	private final Clock clock;

	public LinkService(EntityManager entityManager, PlatformTransactionManager transactionManager,
			AliasGenerator aliasGenerator, UrlValidator urlValidator, AppProperties properties, Clock clock) {
		this.entityManager = entityManager;
		this.transaction = new TransactionTemplate(transactionManager);
		this.aliasGenerator = aliasGenerator;
		this.urlValidator = urlValidator;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Crea un enlace corto para la URL indicada. Cada llamada genera un alias nuevo (ADR-0017).
	 * <p>
	 * Cada intento corre en su propia transacción: si otro pedido simultáneo toma el mismo alias,
	 * la restricción UNIQUE hace fallar solo ese intento y se prueba con otro alias.
	 *
	 * @throws InvalidUrlException       si la URL no es válida (ADR-0019, ADR-0020)
	 * @throws AliasUnavailableException si no se encuentra un alias libre tras el máximo de intentos
	 */
	public Link create(String originalUrl) {
		urlValidator.validate(originalUrl);
		int maxAttempts = properties.alias().maxAttempts();
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			String alias = aliasGenerator.generate();
			try {
				Optional<Link> created = transaction.execute(status -> tryAssign(alias, originalUrl));
				if (created.isPresent()) {
					return created.get();
				}
			} catch (PersistenceException | DataIntegrityViolationException e) {
				if (!isAliasCollision(e)) {
					throw e;
				}
				// carrera con otro pedido por el mismo alias: se reintenta con otro
			}
		}
		throw new AliasUnavailableException(maxAttempts);
	}

	/** Solo se reintenta la violación UNIQUE del alias; otros errores conservan su causa. */
	private static boolean isAliasCollision(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				String constraint = violation.getConstraintName();
				// HSQLDB puede informar el nombre sin esquema o con el esquema PUBLIC.
				return "23505".equals(violation.getSQLState())
						&& (ALIAS_CONSTRAINT.equalsIgnoreCase(constraint)
								|| ("PUBLIC." + ALIAS_CONSTRAINT).equalsIgnoreCase(constraint));
			}
		}
		return false;
	}

	/**
	 * Devuelve el enlace vigente asociado al alias.
	 *
	 * @throws LinkNotFoundException si el alias no existe o el enlace está vencido (no se distinguen, ADR-0018)
	 */
	@Transactional(readOnly = true)
	public Link resolve(String alias) {
		Instant now = clock.instant();
		return findByAlias(alias)
				.filter(link -> !link.isExpired(now))
				.orElseThrow(() -> new LinkNotFoundException(alias));
	}

	/** URL corta pública de un enlace: {@code {app.base-url}/{alias}} (ADR-0022). */
	public String shortUrlOf(Link link) {
		return properties.baseUrl().replaceAll("/+$", "") + "/" + link.getAlias();
	}

	/**
	 * Intenta asignar el alias: lo crea si no existe, o lo reasigna si está vencido (ADR-0021).
	 * Devuelve vacío si el alias pertenece a un enlace vigente.
	 */
	private Optional<Link> tryAssign(String alias, String originalUrl) {
		// la base guarda microsegundos: se trunca para que lo devuelto coincida con lo persistido
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		Instant expiresAt = now.plus(properties.link().ttl());

		Optional<Link> existing = findByAlias(alias);
		if (existing.isEmpty()) {
			Link link = new Link(alias, originalUrl, now, expiresAt);
			entityManager.persist(link);
			entityManager.flush();
			return Optional.of(link);
		}
		if (reassignIfExpired(alias, originalUrl, now, expiresAt)) {
			Link link = existing.get();
			entityManager.refresh(link);
			return Optional.of(link);
		}
		return Optional.empty();
	}

	private Optional<Link> findByAlias(String alias) {
		return entityManager.createQuery("SELECT l FROM Link l WHERE l.alias = :alias", Link.class)
				.setParameter("alias", alias)
				.getResultStream()
				.findFirst();
	}

	/**
	 * Actualización atómica: solo reasigna si el enlace sigue vencido al momento de actualizar,
	 * así dos pedidos simultáneos no pueden quedarse con el mismo alias.
	 */
	private boolean reassignIfExpired(String alias, String originalUrl, Instant now, Instant expiresAt) {
		int updated = entityManager.createQuery("""
				UPDATE Link l
				   SET l.originalUrl = :originalUrl, l.createdAt = :now, l.expiresAt = :expiresAt
				 WHERE l.alias = :alias AND l.expiresAt <= :now
				""")
				.setParameter("originalUrl", originalUrl)
				.setParameter("now", now)
				.setParameter("expiresAt", expiresAt)
				.setParameter("alias", alias)
				.executeUpdate();
		return updated == 1;
	}
}
