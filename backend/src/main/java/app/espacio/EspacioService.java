package app.espacio;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.usuario.SesionRequeridaException;
import app.usuario.Usuario;
import app.usuario.UsuarioRepository;

/** Elegir y cambiar el espacio de nombres del usuario con sesión (ADR-0028 y ADR-0031). */
@Service
public class EspacioService {

	private final EspacioRepository espacioRepository;
	private final UsuarioRepository usuarioRepository;
	private final ValidadorEspacio validador;

	public EspacioService(EspacioRepository espacioRepository, UsuarioRepository usuarioRepository,
			ValidadorEspacio validador) {
		this.espacioRepository = espacioRepository;
		this.usuarioRepository = usuarioRepository;
		this.validador = validador;
	}

	/**
	 * La primera vez crea el espacio; después lo renombra y el nombre anterior queda libre.
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
		}
		return espacio;
	}
}
