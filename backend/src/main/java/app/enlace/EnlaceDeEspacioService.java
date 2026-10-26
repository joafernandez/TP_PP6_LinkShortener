package app.enlace;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import app.common.config.AppProperties;
import app.enlace.alias.GeneradorAlias;
import app.espacio.Espacio;
import app.espacio.EspacioRepository;
import app.usuario.SesionRequeridaException;
import app.usuario.UsuarioRepository;

/**
 * Enlaces dentro del espacio del usuario con sesión (ADR-0032). Reutiliza las reglas del camino anónimo:
 * validación de URL, menor código libre, vigencia de 60 minutos y reasignación atómica de vencidos.
 */
@Service
public class EnlaceDeEspacioService {

	private final EnlaceDeEspacioRepository enlaceRepository;
	private final EspacioRepository espacioRepository;
	private final UsuarioRepository usuarioRepository;
	private final TransactionTemplate transaction;
	private final GeneradorAlias generadorAlias;
	private final ValidadorUrl validadorUrl;
	private final ValidadorCodigo validadorCodigo;
	private final AppProperties propiedades;
	private final Clock clock;

	public EnlaceDeEspacioService(EnlaceDeEspacioRepository enlaceRepository, EspacioRepository espacioRepository,
			UsuarioRepository usuarioRepository, PlatformTransactionManager transactionManager,
			GeneradorAlias generadorAlias, ValidadorUrl validadorUrl, ValidadorCodigo validadorCodigo,
			AppProperties propiedades, Clock clock) {
		this.enlaceRepository = enlaceRepository;
		this.espacioRepository = espacioRepository;
		this.usuarioRepository = usuarioRepository;
		this.transaction = new TransactionTemplate(transactionManager);
		this.generadorAlias = generadorAlias;
		this.validadorUrl = validadorUrl;
		this.validadorCodigo = validadorCodigo;
		this.propiedades = propiedades;
		this.clock = clock;
	}

	/**
	 * Crea un enlace en el espacio del usuario. Sin código personalizado se asigna el menor libre del espacio;
	 * con código, se usa ese si está libre. Cada intento corre en su propia transacción, como en {@link EnlaceService}.
	 *
	 * @throws UrlInvalidaException       si la URL no es válida
	 * @throws CodigoInvalidoException    si el código personalizado no cumple las reglas
	 * @throws EspacioNoElegidoException  si el usuario todavía no eligió su espacio
	 * @throws CodigoOcupadoException     si el código personalizado pertenece a un enlace vigente del espacio
	 * @throws AliasNoDisponibleException si no se encuentra un código libre tras el máximo de intentos
	 */
	public EnlaceDeEspacio crear(String googleSub, String urlOriginal, String codigoPersonalizado) {
		validadorUrl.validar(urlOriginal);
		if (codigoPersonalizado != null && !codigoPersonalizado.isBlank()) {
			String codigo = validadorCodigo.normalizar(codigoPersonalizado.strip());
			return crearConCodigo(googleSub, urlOriginal, codigo);
		}
		int maxIntentos = propiedades.alias().maxIntentos();
		for (int intento = 0; intento < maxIntentos; intento++) {
			try {
				Optional<EnlaceDeEspacio> creado = transaction.execute(status -> {
					Espacio espacio = espacioDe(googleSub);
					Instant ahora = ahora();
					String codigo = generadorAlias.generar(enlaceRepository.buscarAliasVigentes(espacio.getId(), ahora));
					return intentarAsignar(espacio, codigo, urlOriginal, ahora);
				});
				if (creado.isPresent()) {
					return creado.get();
				}
			} catch (ColisionAliasException e) {
				// carrera con otro pedido del mismo espacio: se reintenta con los vigentes actualizados
			}
		}
		throw new AliasNoDisponibleException(maxIntentos);
	}

	/**
	 * Devuelve el enlace vigente. El espacio y el código se tratan en minúsculas.
	 *
	 * @throws EnlaceNoEncontradoException si no existe o está vencido (no se distinguen, ADR-0018)
	 */
	@Transactional(readOnly = true)
	public EnlaceDeEspacio resolver(String espacio, String codigo) {
		Instant ahora = clock.instant();
		return enlaceRepository.buscarPorNombreDeEspacio(espacio.toLowerCase(Locale.ROOT), codigo.toLowerCase(Locale.ROOT))
				.filter(enlace -> !enlace.estaVencido(ahora))
				.orElseThrow(() -> new EnlaceNoEncontradoException(espacio + "/" + codigo));
	}

	/** URL corta pública: {@code {app.base-url}/{espacio}/{código}}. */
	public String urlCortaDe(EnlaceDeEspacio enlace) {
		return propiedades.urlBase().replaceAll("/+$", "") + "/" + enlace.getEspacio().getNombre() + "/" + enlace.getAlias();
	}

	private EnlaceDeEspacio crearConCodigo(String googleSub, String urlOriginal, String codigo) {
		try {
			return transaction.execute(status -> {
				Espacio espacio = espacioDe(googleSub);
				return intentarAsignar(espacio, codigo, urlOriginal, ahora())
						.orElseThrow(() -> new CodigoOcupadoException(codigo));
			});
		} catch (ColisionAliasException e) {
			// otro pedido simultáneo tomó el mismo código
			throw new CodigoOcupadoException(codigo);
		}
	}

	/** Crea el código si no existe o lo reasigna si está vencido; vacío si pertenece a un enlace vigente. */
	private Optional<EnlaceDeEspacio> intentarAsignar(Espacio espacio, String codigo, String urlOriginal, Instant ahora) {
		Instant venceEn = ahora.plus(propiedades.enlace().ttl());
		Optional<EnlaceDeEspacio> existente = enlaceRepository.buscar(espacio.getId(), codigo);
		if (existente.isEmpty()) {
			EnlaceDeEspacio enlace = new EnlaceDeEspacio(espacio, codigo, urlOriginal, ahora, venceEn);
			enlaceRepository.insertar(enlace);
			return Optional.of(enlace);
		}
		EnlaceDeEspacio enlace = existente.get();
		if (enlaceRepository.reasignarSiEstaVencido(enlace, urlOriginal, ahora, venceEn)) {
			return Optional.of(enlace);
		}
		return Optional.empty();
	}

	private Espacio espacioDe(String googleSub) {
		Long usuarioId = usuarioRepository.buscarPorGoogleSub(googleSub)
				.orElseThrow(SesionRequeridaException::new)
				.getId();
		return espacioRepository.buscarPorUsuario(usuarioId).orElseThrow(EspacioNoElegidoException::new);
	}

	/** La base guarda microsegundos: se trunca para que lo devuelto coincida con lo persistido. */
	private Instant ahora() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}
}
