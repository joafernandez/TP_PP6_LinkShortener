package app.espacio;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.enlace.EnlaceDeEspacioRepository;
import app.usuario.SesionRequeridaException;
import app.usuario.Usuario;
import app.usuario.UsuarioRepository;

/** Elegir y cambiar el espacio de nombres del usuario con sesión (ADR-0028 y ADR-0031). */
@Service
public class EspacioService {

	private final EspacioRepository espacioRepository;
	private final UsuarioRepository usuarioRepository;
	private final EnlaceDeEspacioRepository enlaceDeEspacioRepository;
	private final ValidadorEspacio validador;
	private final Clock clock;

	public EspacioService(EspacioRepository espacioRepository, UsuarioRepository usuarioRepository,
			EnlaceDeEspacioRepository enlaceDeEspacioRepository, ValidadorEspacio validador, Clock clock) {
		this.espacioRepository = espacioRepository;
		this.usuarioRepository = usuarioRepository;
		this.enlaceDeEspacioRepository = enlaceDeEspacioRepository;
		this.validador = validador;
		this.clock = clock;
	}

	/**
	 * La primera vez crea el espacio; después lo renombra, el nombre anterior queda libre y los enlaces
	 * vigentes del espacio se vencen en la misma transacción: dejan de funcionar y no pasan al nombre nuevo.
	 * La unicidad la garantiza la base aunque dos pedidos lleguen a la vez.
	 *
	 * @throws NombreEspacioInvalidoException si el nombre no cumple las reglas
	 * @throws EspacioOcupadoException        si el nombre pertenece a otro usuario
	 * @throws SesionRequeridaException       si el usuario de la sesión no está registrado
	 */
	@Transactional
	public Espacio elegir(String googleSub, String nombrePedido) {
		String nombre = validador.normalizar(nombrePedido);
		Usuario usuario = usuarioRepository.buscarPorGoogleSub(googleSub)
				.orElseThrow(SesionRequeridaException::new);

		Optional<Espacio> actual = espacioRepository.buscarPorUsuario(usuario.getId());
		if (actual.isEmpty()) {
			Espacio espacio = new Espacio(nombre, usuario);
			espacioRepository.insertar(espacio);
			return espacio;
		}
		Espacio espacio = actual.get();
		if (!espacio.getNombre().equals(nombre)) {
			espacio.renombrar(nombre);
			espacioRepository.guardarCambioDeNombre(espacio);
			enlaceDeEspacioRepository.vencerVigentes(espacio.getId(), clock.instant().truncatedTo(ChronoUnit.MICROS));
		}
		return espacio;
	}
}
