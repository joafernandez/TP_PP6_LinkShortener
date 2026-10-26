package app.espacio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.usuario.SesionRequeridaException;
import app.usuario.Usuario;
import app.usuario.UsuarioRepository;

/** Reglas para elegir y cambiar el espacio, con los Repository simulados. */
class EspacioServiceTest {

	private final EspacioRepository espacioRepository = mock(EspacioRepository.class);
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
	private final ValidadorEspacio validador = mock(ValidadorEspacio.class);
	private final EspacioService service = new EspacioService(espacioRepository, usuarioRepository, validador);
	private Usuario usuario;

	@BeforeEach
	void setUp() throws Exception {
		usuario = new Usuario("google-123", "joana@example.org", "Joana", Instant.EPOCH);
		Field id = Usuario.class.getDeclaredField("id");
		id.setAccessible(true);
		id.set(usuario, 7L);
		when(usuarioRepository.buscarPorGoogleSub("google-123")).thenReturn(Optional.of(usuario));
		when(validador.normalizar(any())).thenAnswer(invocacion -> invocacion.getArgument(0, String.class).toLowerCase());
	}

	@Test
	void laPrimeraVezCreaElEspacio() {
		when(espacioRepository.buscarPorUsuario(7L)).thenReturn(Optional.empty());

		Espacio espacio = service.elegir("google-123", "Joana23");

		assertThat(espacio.getNombre()).isEqualTo("joana23");
		assertThat(espacio.getUsuario()).isSameAs(usuario);
		verify(espacioRepository).insertar(espacio);
	}

	@Test
	void despuesLoRenombraSinCrearOtro() {
		Espacio actual = new Espacio("joana", usuario);
		when(espacioRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(actual));

		assertThat(service.elegir("google-123", "joana23")).isSameAs(actual);
		assertThat(actual.getNombre()).isEqualTo("joana23");
		verify(espacioRepository).guardarCambioDeNombre(actual);
		verify(espacioRepository, never()).insertar(any());
	}

	@Test
	void elegirElMismoNombreNoCambiaNada() {
		Espacio actual = new Espacio("joana", usuario);
		when(espacioRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(actual));

		assertThat(service.elegir("google-123", "JOANA")).isSameAs(actual);
		verify(espacioRepository, never()).guardarCambioDeNombre(any());
		verify(espacioRepository, never()).insertar(any());
	}

	@Test
	void unNombreInvalidoSeRechazaAntesDeTocarLaBase() {
		when(validador.normalizar("mal nombre")).thenThrow(new NombreEspacioInvalidoException("inválido"));

		assertThatThrownBy(() -> service.elegir("google-123", "mal nombre"))
				.isInstanceOf(NombreEspacioInvalidoException.class);
		verifyNoInteractions(espacioRepository);
	}

	@Test
	void unUsuarioNoRegistradoDebeIniciarSesion() {
		when(usuarioRepository.buscarPorGoogleSub("desconocido")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.elegir("desconocido", "joana"))
				.isInstanceOf(SesionRequeridaException.class);
		verifyNoInteractions(espacioRepository);
	}
}
