package app.espacio;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.common.config.AppProperties;
import app.usuario.SesionRequeridaException;
import app.usuario.Usuario;
import app.usuario.UsuarioRepository;

/** Elegir y cambiar el espacio de nombres del usuario con sesión (ADR-0028, ADR-0031 y ADR-0034). */
@Service
public class EspacioService {

	private final EspacioRepository espacioRepository;
	private final NombreDeEspacioRepository nombreRepository;
	private final UsuarioRepository usuarioRepository;
	private final ValidadorEspacio validador;
	private final AppProperties propiedades;
	private final Clock clock;

	public EspacioService(EspacioRepository espacioRepository, NombreDeEspacioRepository nombreRepository,
			UsuarioRepository usuarioRepository, ValidadorEspacio validador, AppProperties propiedades, Clock clock) {
		this.espacioRepository = espacioRepository;
		this.nombreRepository = nombreRepository;
		this.usuarioRepository = usuarioRepository;
		this.validador = validador;
		this.propiedades = propiedades;
		this.clock = clock;
	}

	/**
	 * La primera vez crea el espacio. Después lo cambia: el nombre nuevo funciona en el acto con todos los enlaces,
	 * y el anterior sigue funcionando y reservado durante la transición (60 minutos). Los enlaces no se tocan:
	 * siguen atados al espacio y conservan su vencimiento. Todo ocurre en una transacción, y la unicidad de los
	 * nombres la garantiza la base aunque dos pedidos lleguen a la vez.
	 *
	 * @throws NombreEspacioInvalidoException si el nombre no cumple las reglas
	 * @throws EspacioOcupadoException        si el nombre pertenece o está reservado para otro espacio
	 * @throws SesionRequeridaException       si el usuario de la sesión no está registrado
	 */
	@Transactional
	public Espacio elegir(String googleSub, String nombrePedido) {
		String nombre = validador.normalizar(nombrePedido);
		Usuario usuario = usuarioRepository.buscarPorGoogleSub(googleSub)
				.orElseThrow(SesionRequeridaException::new);
		Instant ahora = ahora();

		Optional<Espacio> actual = espacioRepository.buscarPorUsuario(usuario.getId());
		if (actual.isEmpty()) {
			Espacio espacio = new Espacio(nombre, usuario);
			espacioRepository.insertar(espacio);
			reclamar(espacio, nombre, ahora);
			return espacio;
		}
		Espacio espacio = actual.get();
		if (espacio.getNombre().equals(nombre)) {
			return espacio;
		}
		reclamar(espacio, nombre, ahora);
		// el principal que se abandona empieza su transición; los demás anteriores conservan su vencimiento
		nombreRepository.buscar(espacio.getNombre())
				.ifPresent(anterior -> anterior.iniciarTransicion(ahora.plus(propiedades.espacio().transicion())));
		nombreRepository.guardarCambios(nombre);
		espacio.renombrar(nombre);
		espacioRepository.guardarCambioDeNombre(espacio);
		return espacio;
	}

	/** Nombres anteriores del espacio que siguen funcionando, del que vence antes al que vence después. */
	@Transactional(readOnly = true)
	public List<NombreAnterior> anterioresVigentes(Long espacioId) {
		return nombreRepository.buscarAnterioresVigentes(espacioId, clock.instant()).stream()
				.map(anterior -> new NombreAnterior(anterior.getNombre(), anterior.getVenceEn()))
				.toList();
	}

	/**
	 * Deja {@code nombre} como principal del espacio: lo registra si es libre, lo recupera si es un nombre propio
	 * todavía reservado, o lo toma si su transición ya venció.
	 *
	 * @throws EspacioOcupadoException si es el principal o un nombre reservado de otro espacio
	 */
	private void reclamar(Espacio espacio, String nombre, Instant ahora) {
		Optional<NombreDeEspacio> existente = nombreRepository.buscar(nombre);
		if (existente.isEmpty()) {
			nombreRepository.insertar(new NombreDeEspacio(espacio, nombre));
			return;
		}
		NombreDeEspacio registrado = existente.get();
		if (registrado.getEspacio().getId().equals(espacio.getId()) && registrado.estaVigente(ahora)) {
			registrado.hacerPrincipal();
			return;
		}
		if (!nombreRepository.reasignarSiEstaVencido(registrado, espacio, ahora)) {
			throw new EspacioOcupadoException(nombre, null);
		}
	}

	/** La base guarda microsegundos: se trunca para que lo devuelto coincida con lo persistido. */
	private Instant ahora() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}
}
