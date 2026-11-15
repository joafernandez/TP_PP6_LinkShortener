package app.enlace;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.time.Duration;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import app.support.MutableClock;
import app.support.TestClockConfig;
import app.usuario.UsuarioService;

/** «Mis enlaces»: {@code GET /api/v1/me/links} con la seguridad real y el login de Google simulado. */
@SpringBootTest
@Import(TestClockConfig.class)
class MisEnlacesIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	private Cookie xsrf;

	@BeforeEach
	void setUp() throws Exception {
		mvc = webAppContextSetup(context).apply(springSecurity()).build();
		clock.set(TestClockConfig.T0);
		jdbc.update("DELETE FROM namespace_link");
		jdbc.update("DELETE FROM namespace_name");
		jdbc.update("DELETE FROM namespace");
		jdbc.update("DELETE FROM app_user");
		jdbc.update("DELETE FROM link");
		usuarioService.registrarSiEsNuevo("google-joana", "joana@example.org", "Joana");
		usuarioService.registrarSiEsNuevo("google-ana", "ana@example.org", "Ana");
		xsrf = mvc.perform(get("/")).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}

	@Test
	void muestraLosEnlacesPropiosDelMasRecienteAlMasAntiguoConTodosSusDatos() throws Exception {
		elegirEspacio("google-joana", "joana");
		acortar("google-joana", "https://example.org/primero", null);
		clock.advance(Duration.ofMinutes(5));
		acortar("google-joana", "https://example.org/segundo", "final");

		mvc.perform(get("/api/v1/me/links").with(sesion("google-joana")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].alias").value("final"))
				.andExpect(jsonPath("$[0].shortUrl").value("http://short.test/joana/final"))
				.andExpect(jsonPath("$[0].originalUrl").value("https://example.org/segundo"))
				.andExpect(jsonPath("$[0].createdAt").value(TestClockConfig.T0.plus(Duration.ofMinutes(5)).toString()))
				.andExpect(jsonPath("$[0].expiresAt").value(TestClockConfig.T0.plus(Duration.ofMinutes(65)).toString()))
				.andExpect(jsonPath("$[1].alias").value("1"));
	}

	@Test
	void cadaUsuarioVeSoloSusEnlacesYNoLosAnonimos() throws Exception {
		elegirEspacio("google-joana", "joana");
		elegirEspacio("google-ana", "ana");
		acortar("google-joana", "https://example.org/de-joana", null);
		acortar("google-ana", "https://example.org/de-ana", null);
		mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\": \"https://example.org/anonimo\"}")).andExpect(status().isCreated());

		mvc.perform(get("/api/v1/me/links").with(sesion("google-joana")))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].originalUrl").value("https://example.org/de-joana"));
	}

	@Test
	void incluyeLosVencidosQueSiguenEnLaBase() throws Exception {
		elegirEspacio("google-joana", "joana");
		acortar("google-joana", "https://example.org/vencido", null);
		clock.advance(Duration.ofMinutes(61));

		mvc.perform(get("/api/v1/me/links").with(sesion("google-joana")))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].expiresAt").value(TestClockConfig.T0.plus(Duration.ofMinutes(60)).toString()));
	}

	@Test
	void despuesDeCambiarElEspacioLasUrlCortasUsanElNombreNuevo() throws Exception {
		elegirEspacio("google-joana", "joana");
		acortar("google-joana", "https://example.org/destino", null);
		elegirEspacio("google-joana", "joana23");

		mvc.perform(get("/api/v1/me/links").with(sesion("google-joana")))
				.andExpect(jsonPath("$[0].shortUrl").value("http://short.test/joana23/1"));
	}

	@Test
	void sinEspacioElegidoOSinEnlacesLaListaEstaVacia() throws Exception {
		mvc.perform(get("/api/v1/me/links").with(sesion("google-joana")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		elegirEspacio("google-joana", "joana");
		mvc.perform(get("/api/v1/me/links").with(sesion("google-joana")))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void sinSesionResponde401() throws Exception {
		mvc.perform(get("/api/v1/me/links"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	private void elegirEspacio(String googleSub, String nombre) throws Exception {
		mvc.perform(put("/api/v1/me/namespace").with(sesion(googleSub))
						.cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content("{\"namespace\": \"" + nombre + "\"}"))
				.andExpect(status().isOk());
	}

	private void acortar(String googleSub, String url, String codigo) throws Exception {
		String cuerpo = codigo == null
				? "{\"url\": \"" + url + "\"}"
				: "{\"url\": \"" + url + "\", \"code\": \"" + codigo + "\"}";
		mvc.perform(post("/api/v1/me/links").with(sesion(googleSub))
						.cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated());
	}

	private static OidcLoginRequestPostProcessor sesion(String googleSub) {
		return oidcLogin().idToken(token -> token.subject(googleSub));
	}
}
