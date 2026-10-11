package app.enlace;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import app.common.config.AppProperties;
import app.enlace.alias.GeneradorAlias;

/**
 * Reglas de negocio de los enlaces cortos.
 * Coordina las transacciones y delega la persistencia en EnlaceRepository (ADR-0024).
 */
@Service
public class EnlaceService {

	private final EnlaceRepository enlaceRepository;
	private final TransactionTemplate transaction;
	private final GeneradorAlias generadorAlias;
	private final ValidadorUrl validadorUrl;
	private final AppProperties propiedades;
	private final Clock clock;

	public EnlaceService(EnlaceRepository enlaceRepository, PlatformTransactionManager transactionManager,
			GeneradorAlias generadorAlias, ValidadorUrl validadorUrl, AppProperties propiedades, Clock clock) {
		this.enlaceRepository = enlaceRepository;
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
			} catch (ColisionAliasException e) {
				// carrera con otro pedido por el mismo alias: se reintenta con otro
			}
		}
		throw new AliasNoDisponibleException(maxIntentos);
	}

	/**
	 * Devuelve el enlace vigente asociado al alias.
	 *
	 * @throws EnlaceNoEncontradoException si el alias no existe o el enlace está vencido (no se distinguen, ADR-0018)
	 */
	@Transactional(readOnly = true)
	public Enlace resolver(String alias) {
		Instant ahora = clock.instant();
		return enlaceRepository.buscarPorAlias(alias)
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

		Optional<Enlace> existente = enlaceRepository.buscarPorAlias(alias);
		if (existente.isEmpty()) {
			Enlace enlace = new Enlace(alias, urlOriginal, ahora, venceEn);
			enlaceRepository.insertar(enlace);
			return Optional.of(enlace);
		}
		Enlace enlace = existente.get();
		if (enlaceRepository.reasignarSiEstaVencido(enlace, urlOriginal, ahora, venceEn)) {
			return Optional.of(enlace);
		}
		return Optional.empty();
	}
}
