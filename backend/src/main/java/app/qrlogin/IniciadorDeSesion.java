package app.qrlogin;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import app.usuario.Usuario;

/**
 * Abre la sesión de un usuario sin pasar por Google, para el login con QR (ADR-0033).
 * Crea el mismo tipo de principal que el login con Google ({@code OidcUser}), así el resto de los
 * endpoints no distingue cómo ingresó el usuario.
 */
@Component
public class IniciadorDeSesion {

	private static final String PROVEEDOR = "google";
	private static final long VIGENCIA_DEL_TOKEN_SEGUNDOS = 3600;

	private final SecurityContextRepository repositorioDeContexto = new HttpSessionSecurityContextRepository();
	private final SecurityContextHolderStrategy estrategia = SecurityContextHolder.getContextHolderStrategy();
	private final Clock clock;

	public IniciadorDeSesion(Clock clock) {
		this.clock = clock;
	}

	/** Guarda la sesión en la cookie de quien hizo el pedido. Si ya tenía una sesión, cambia su identificador. */
	public void iniciar(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
		if (request.getSession(false) != null) {
			request.changeSessionId(); // evita que una sesión previa quede fijada al nuevo usuario
		}
		Instant ahora = clock.instant();
		OidcIdToken token = new OidcIdToken("qr-login", ahora, ahora.plusSeconds(VIGENCIA_DEL_TOKEN_SEGUNDOS),
				reclamos(usuario));
		DefaultOidcUser principal = new DefaultOidcUser(List.of(new OidcUserAuthority(token)), token);
		OAuth2AuthenticationToken autenticacion =
				new OAuth2AuthenticationToken(principal, principal.getAuthorities(), PROVEEDOR);

		SecurityContext contexto = estrategia.createEmptyContext();
		contexto.setAuthentication(autenticacion);
		estrategia.setContext(contexto);
		repositorioDeContexto.saveContext(contexto, request, response);
	}

	private static Map<String, Object> reclamos(Usuario usuario) {
		Map<String, Object> reclamos = new HashMap<>();
		reclamos.put("sub", usuario.getGoogleSub());
		if (usuario.getEmail() != null) {
			reclamos.put("email", usuario.getEmail());
		}
		if (usuario.getNombre() != null) {
			reclamos.put("name", usuario.getNombre());
		}
		return reclamos;
	}
}
