package app.usuario;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.espacio.Espacio;
import app.espacio.EspacioRepository;
import app.espacio.EspacioService;
import app.espacio.NombreAnterior;

/** Reglas de los usuarios que inician sesión con Google (ADR-0028 y ADR-0030). */
@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final EspacioRepository espacioRepository;
	private final EspacioService espacioService;
	private final Clock clock;

	public UsuarioService(UsuarioRepository usuarioRepository, EspacioRepository espacioRepository,
			EspacioService espacioService, Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.espacioRepository = espacioRepository;
		this.espacioService = espacioService;
		this.clock = clock;
	}

	/** En el primer login crea el usuario; en los siguientes devuelve el existente, sin duplicarlo. */
	@Transactional
	public Usuario registrarSiEsNuevo(String googleSub, String email, String nombre) {
		return usuarioRepository.buscarPorGoogleSub(googleSub).orElseGet(() -> {
			// la base guarda microsegundos, igual que en los enlaces
			Usuario usuario = new Usuario(googleSub, email, nombre, clock.instant().truncatedTo(ChronoUnit.MICROS));
			usuarioRepository.insertar(usuario);
			return usuario;
		});
	}

	/**
	 * Datos del usuario con sesión, incluido su espacio si ya lo eligió y los nombres anteriores que siguen
	 * en transición (ADR-0034).
	 *
	 * @throws SesionRequeridaException si el usuario de la sesión no está registrado
	 */
	@Transactional(readOnly = true)
	public PerfilUsuario perfil(String googleSub) {
		Usuario usuario = usuarioRepository.buscarPorGoogleSub(googleSub)
				.orElseThrow(SesionRequeridaException::new);
		Optional<Espacio> espacio = espacioRepository.buscarPorUsuario(usuario.getId());
		List<NombreAnterior> anteriores = espacio
				.map(encontrado -> espacioService.anterioresVigentes(encontrado.getId()))
				.orElse(List.of());
		return new PerfilUsuario(usuario.getNombre(), usuario.getEmail(), espacio.map(Espacio::getNombre).orElse(null),
				anteriores);
	}
}
