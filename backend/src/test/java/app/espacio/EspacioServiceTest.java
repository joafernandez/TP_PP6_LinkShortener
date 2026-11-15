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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;
import app.common.config.AppProperties.EnlaceSettings;
import app.common.config.AppProperties.EspacioSettings;
import app.usuario.SesionRequeridaException;
import app.usuario.Usuario;
import app.usuario.UsuarioRepository;

/** Reglas para elegir y cambiar el espacio con transición del nombre anterior (ADR-0034), con los Repository simulados. */
class EspacioServiceTest {

	private static final Instant AHORA = Instant.parse("2026-11-15T14:00:00Z");
	private static final Duration TRANSICION = Duration.ofMinutes(60);

	private final EspacioRepository espacioRepository = mock(EspacioRepository.class);
	private final NombreDeEspacioRepository nombreRepository = mock(NombreDeEspacioRepository.class);
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
	private final ValidadorEspacio validador = mock(ValidadorEspacio.class);
	private final AppProperties propiedades = new AppProperties("http://short.test",
			new EnlaceSettings(Duration.ofMinutes(60)), new AliasSettings("abc", 10, Set.of()),
			new EspacioSettings(Set.of(), TRANSICION));
	private final EspacioService service = new EspacioService(espacioRepository, nombreRepository, usuarioRepository,
			validador, propiedades, Clock.fixed(AHORA, ZoneOffset.UTC));
	private Usuario usuario;

	@BeforeEach
	void setUp() throws Exception {
		usuario = conId(new Usuario("google-123", "sofia@example.org", "Sofía", Instant.EPOCH), Usuario.class, 7L);
		when(usuarioRepository.buscarPorGoogleSub("google-123")).thenReturn(Optional.of(usuario));
		when(validador.normalizar(any())).thenAnswer(invocacion -> invocacion.getArgument(0, String.class).toLowerCase());
	}

	@Test
	void laPrimeraVezCreaElEspacioYRegistraSuNombrePrincipal() {
		when(espacioRepository.buscarPorUsuario(7L)).thenReturn(Optional.empty());
		when(nombreRepository.buscar("sofi")).thenReturn(Optional.empty());

		Espacio espacio = service.elegir("google-123", "Sofi");

		assertThat(espacio.getNombre()).isEqualTo("sofi");
		verify(espacioRepository).insertar(espacio);
		ArgumentCaptor<NombreDeEspacio> registrado = ArgumentCaptor.forClass(NombreDeEspacio.class);
		verify(nombreRepository).insertar(registrado.capture());
		assertThat(registrado.getValue().getNombre()).isEqualTo("sofi");
		assertThat(registrado.getValue().esPrincipal()).isTrue();
	}

	@Test
	void alCambiarElNuevoEsPrincipalYElAnteriorEmpiezaSuTransicion() throws Exception {
		Espacio espacio = espacio("sofi");
		NombreDeEspacio anterior = new NombreDeEspacio(espacio, "sofi");
		when(nombreRepository.buscar("sofia")).thenReturn(Optional.empty());
		when(nombreRepository.buscar("sofi")).thenReturn(Optional.of(anterior));

		assertThat(service.elegir("google-123", "sofia")).isSameAs(espacio);

		assertThat(espacio.getNombre()).isEqualTo("sofia");
		assertThat(anterior.getVenceEn()).isEqualTo(AHORA.plus(TRANSICION));
		verify(nombreRepository).insertar(any(NombreDeEspacio.class));
		verify(espacioRepository).guardarCambioDeNombre(espacio);
	}

	@Test
	void puedeRecuperarUnNombrePropioTodaviaReservado() throws Exception {
		Espacio espacio = espacio("sofia");
		NombreDeEspacio reservado = new NombreDeEspacio(espacio, "sofi");
		reservado.iniciarTransicion(AHORA.plus(Duration.ofMinutes(20)));
		NombreDeEspacio principal = new NombreDeEspacio(espacio, "sofia");
		when(nombreRepository.buscar("sofi")).thenReturn(Optional.of(reservado));
		when(nombreRepository.buscar("sofia")).thenReturn(Optional.of(principal));

		service.elegir("google-123", "sofi");

		assertThat(espacio.getNombre()).isEqualTo("sofi");
		assertThat(reservado.esPrincipal()).isTrue();
		assertThat(principal.getVenceEn()).isEqualTo(AHORA.plus(TRANSICION));
		verify(nombreRepository, never()).insertar(any());
		verify(nombreRepository, never()).reasignarSiEstaVencido(any(), any(), any());
	}

	@Test
	void unNombreVigenteDeOtroEspacioEstaOcupadoYNoCambiaNada() throws Exception {
		Espacio espacio = espacio("sofi");
		Espacio otro = conId(new Espacio("ana", usuario), Espacio.class, 99L);
		NombreDeEspacio ajeno = new NombreDeEspacio(otro, "ana");
		when(nombreRepository.buscar("ana")).thenReturn(Optional.of(ajeno));
		when(nombreRepository.reasignarSiEstaVencido(ajeno, espacio, AHORA)).thenReturn(false);

		assertThatThrownBy(() -> service.elegir("google-123", "ana"))
				.isInstanceOf(EspacioOcupadoException.class)
				.hasMessageContaining("ana");
		assertThat(espacio.getNombre()).isEqualTo("sofi");
		verify(espacioRepository, never()).guardarCambioDeNombre(any());
	}

	@Test
	void unNombreCuyaTransicionVencioSePuedeTomar() throws Exception {
		Espacio espacio = espacio("sofi");
		Espacio otro = conId(new Espacio("ana2", usuario), Espacio.class, 99L);
		NombreDeEspacio vencido = new NombreDeEspacio(otro, "ana");
		vencido.iniciarTransicion(AHORA.minusSeconds(1));
		when(nombreRepository.buscar("ana")).thenReturn(Optional.of(vencido));
		when(nombreRepository.reasignarSiEstaVencido(vencido, espacio, AHORA)).thenReturn(true);

		service.elegir("google-123", "ana");

		assertThat(espacio.getNombre()).isEqualTo("ana");
		verify(espacioRepository).guardarCambioDeNombre(espacio);
	}

	@Test
	void elegirElMismoNombreNoCambiaNada() throws Exception {
		Espacio espacio = espacio("sofi");

		assertThat(service.elegir("google-123", "SOFI")).isSameAs(espacio);
		verifyNoInteractions(nombreRepository);
		verify(espacioRepository, never()).guardarCambioDeNombre(any());
	}

	@Test
	void unNombreInvalidoSeRechazaAntesDeTocarLaBase() {
		when(validador.normalizar("mal nombre")).thenThrow(new NombreEspacioInvalidoException("inválido"));

		assertThatThrownBy(() -> service.elegir("google-123", "mal nombre"))
				.isInstanceOf(NombreEspacioInvalidoException.class);
		verifyNoInteractions(espacioRepository, nombreRepository);
	}

	@Test
	void unUsuarioNoRegistradoDebeIniciarSesion() {
		when(usuarioRepository.buscarPorGoogleSub("desconocido")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.elegir("desconocido", "sofi"))
				.isInstanceOf(SesionRequeridaException.class);
		verifyNoInteractions(espacioRepository, nombreRepository);
	}

	/** Espacio actual del usuario, con id como si ya estuviera guardado. */
	private Espacio espacio(String nombre) throws Exception {
		Espacio espacio = conId(new Espacio(nombre, usuario), Espacio.class, 1L);
		when(espacioRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(espacio));
		return espacio;
	}

	/** El id lo asigna la base; en un test unitario se fija por reflexión. */
	private static <T> T conId(T entidad, Class<T> tipo, Long id) throws Exception {
		Field campo = tipo.getDeclaredField("id");
		campo.setAccessible(true);
		campo.set(entidad, id);
		return entidad;
	}
}
