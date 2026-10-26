package app.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Conecta el login de Google con {@link UsuarioService}: Spring Security valida a Google
 * y obtiene los datos del usuario, y este servicio lo registra en la base (ADR-0030).
 */
@Component
public class UsuarioOidcService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private final OAuth2UserService<OidcUserRequest, OidcUser> google;
	private final UsuarioService usuarioService;

	@Autowired
	public UsuarioOidcService(UsuarioService usuarioService) {
		this(new OidcUserService(), usuarioService);
	}

	UsuarioOidcService(OAuth2UserService<OidcUserRequest, OidcUser> google, UsuarioService usuarioService) {
		this.google = google;
		this.usuarioService = usuarioService;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest pedido) throws OAuth2AuthenticationException {
		OidcUser usuarioGoogle = google.loadUser(pedido);
		usuarioService.registrarSiEsNuevo(usuarioGoogle.getSubject(), usuarioGoogle.getEmail(),
				usuarioGoogle.getFullName());
		return usuarioGoogle;
	}
}
