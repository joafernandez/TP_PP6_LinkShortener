package app.qrlogin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import app.usuario.Usuario;

class IniciadorDeSesionTest {

	private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");

	private final IniciadorDeSesion iniciador = new IniciadorDeSesion(Clock.fixed(AHORA, ZoneOffset.UTC));
	private final MockHttpServletRequest request = new MockHttpServletRequest();
	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void guardaElUsuarioComoPrincipalOidcEnLaSesion() {
		iniciador.iniciar(new Usuario("sub-1", "a@b.org", "Ana", AHORA), request, response);

		SecurityContext guardado = (SecurityContext) request.getSession(false).getAttribute("SPRING_SECURITY_CONTEXT");
		OidcUser principal = (OidcUser) guardado.getAuthentication().getPrincipal();
		assertThat(principal.getSubject()).isEqualTo("sub-1");
		assertThat(principal.getEmail()).isEqualTo("a@b.org");
		assertThat(principal.getFullName()).isEqualTo("Ana");
	}

	@Test
	void aceptaUsuariosSinEmailNiNombre() {
		iniciador.iniciar(new Usuario("sub-2", null, null, AHORA), request, response);

		SecurityContext guardado = (SecurityContext) request.getSession(false).getAttribute("SPRING_SECURITY_CONTEXT");
		OidcUser principal = (OidcUser) guardado.getAuthentication().getPrincipal();
		assertThat(principal.getSubject()).isEqualTo("sub-2");
		assertThat(principal.getEmail()).isNull();
	}

	@Test
	void conSesionPreviaCambiaSuIdentificador() {
		String previo = request.getSession(true).getId();

		iniciador.iniciar(new Usuario("sub-3", "c@d.org", "Cris", AHORA), request, response);

		assertThat(request.getSession(false).getId()).isNotEqualTo(previo);
	}
}
