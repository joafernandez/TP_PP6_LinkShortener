package app.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.espacio.Espacio;
import app.espacio.EspacioRepository;

/** Reglas del registro de usuarios, sin JPA ni Google. */
class UsuarioServiceTest {

	private static final Instant AHORA = Instant.parse("2026-10-25T18:00:00.123456789Z");
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
	private final EspacioRepository espacioRepository = mock(EspacioRepository.class);
	private final UsuarioService service = new UsuarioService(usuarioRepository, espacioRepository,
			Clock.fixed(AHORA, ZoneOffset.UTC));

	@Test
	void elPrimerLoginCreaElUsuarioConLaFechaDelReloj() {
		when(usuarioRepository.buscarPorGoogleSub("google-123")).thenReturn(Optional.empty());

		Usuario creado = service.registrarSiEsNuevo("google-123", "melina@example.org", "Melina");

		assertThat(creado.getGoogleSub()).isEqualTo("google-123");
		assertThat(creado.getEmail()).isEqualTo("melina@example.org");
		assertThat(creado.getNombre()).isEqualTo("Melina");
		assertThat(creado.getCreadoEn()).isEqualTo(Instant.parse("2026-10-25T18:00:00.123456Z"));
		verify(usuarioRepository).insertar(creado);
	}

	@Test
	void losLoginsSiguientesNoDuplicanElUsuario() {
		Usuario existente = new Usuario("google-123", "melina@example.org", "Melina", AHORA);
		when(usuarioRepository.buscarPorGoogleSub("google-123")).thenReturn(Optional.of(existente));

		assertThat(service.registrarSiEsNuevo("google-123", "melina@example.org", "Melina")).isSameAs(existente);
		verify(usuarioRepository, never()).insertar(any());
	}

	@Test
	void elPerfilIncluyeElEspacioSiYaLoEligio() throws Exception {
		Usuario usuario = usuarioConId(7L);
		when(usuarioRepository.buscarPorGoogleSub("google-123")).thenReturn(Optional.of(usuario));
		when(espacioRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(new Espacio("melina", usuario)));

		assertThat(service.perfil("google-123"))
				.isEqualTo(new PerfilUsuario("Melina", "melina@example.org", "melina"));
	}

	@Test
	void elPerfilTieneEspacioNuloSiTodaviaNoLoEligio() throws Exception {
		when(usuarioRepository.buscarPorGoogleSub("google-123")).thenReturn(Optional.of(usuarioConId(7L)));
		when(espacioRepository.buscarPorUsuario(7L)).thenReturn(Optional.empty());

		assertThat(service.perfil("google-123").espacio()).isNull();
	}

	@Test
	void elPerfilDeUnUsuarioNoRegistradoPideIniciarSesion() {
		when(usuarioRepository.buscarPorGoogleSub("google-123")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.perfil("google-123")).isInstanceOf(SesionRequeridaException.class);
	}

	/** El id lo asigna la base; en un test unitario se fija por reflexión. */
	private static Usuario usuarioConId(Long id) throws Exception {
		Usuario usuario = new Usuario("google-123", "melina@example.org", "Melina", AHORA);
		Field campo = Usuario.class.getDeclaredField("id");
		campo.setAccessible(true);
		campo.set(usuario, id);
		return usuario;
	}
}
