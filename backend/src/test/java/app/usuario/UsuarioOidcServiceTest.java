package app.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** El login de Google registra al usuario en la base; si Google falla, no se registra nada. */
class UsuarioOidcServiceTest {

	@SuppressWarnings("unchecked")
	private final OAuth2UserService<OidcUserRequest, OidcUser> google = mock(OAuth2UserService.class);
	private final UsuarioService usuarioService = mock(UsuarioService.class);
	private final UsuarioOidcService service = new UsuarioOidcService(google, usuarioService);
	private final OidcUserRequest pedido = mock(OidcUserRequest.class);

	@Test
	void registraAlUsuarioConLosDatosDeGoogleYLoDevuelve() {
		OidcUser usuarioGoogle = usuarioGoogle("google-123", "melina@example.org", "Melina");
		when(google.loadUser(pedido)).thenReturn(usuarioGoogle);

		assertThat(service.loadUser(pedido)).isSameAs(usuarioGoogle);
		verify(usuarioService).registrarSiEsNuevo("google-123", "melina@example.org", "Melina");
	}

	@Test
	void siGoogleFallaNoRegistraNada() {
		when(google.loadUser(pedido)).thenThrow(new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));

		assertThatThrownBy(() -> service.loadUser(pedido)).isInstanceOf(OAuth2AuthenticationException.class);
		verify(usuarioService, never()).registrarSiEsNuevo(any(), any(), any());
	}

	private static OidcUser usuarioGoogle(String sub, String email, String nombre) {
		OidcIdToken token = new OidcIdToken("token", Instant.EPOCH, Instant.EPOCH.plusSeconds(60),
				Map.of("sub", sub, "email", email, "name", nombre));
		return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
	}
}
