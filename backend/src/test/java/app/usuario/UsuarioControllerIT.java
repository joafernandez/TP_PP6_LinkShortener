package app.usuario;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/** {@code GET /api/v1/me} con la seguridad real y el login de Google simulado. */
@SpringBootTest
class UsuarioControllerIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup(context).apply(springSecurity()).build();
		jdbc.update("DELETE FROM namespace");
		jdbc.update("DELETE FROM app_user");
	}

	@Test
	void sinSesionResponde401ConProblemDetail() throws Exception {
		mvc.perform(get("/api/v1/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("Iniciá sesión con Google para continuar."));
	}

	@Test
	void conSesionDevuelveLosDatosDelUsuarioSinEspacio() throws Exception {
		usuarioService.registrarSiEsNuevo("google-123", "melina@example.org", "Melina");

		mvc.perform(get("/api/v1/me").with(sesionDe("google-123", "melina@example.org", "Melina")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Melina"))
				.andExpect(jsonPath("$.email").value("melina@example.org"))
				.andExpect(jsonPath("$.namespace").isEmpty());
	}

	@Test
	void conSesionDevuelveElEspacioSiYaLoEligio() throws Exception {
		Usuario usuario = usuarioService.registrarSiEsNuevo("google-123", "melina@example.org", "Melina");
		jdbc.update("INSERT INTO namespace (name, user_id) VALUES ('melina', ?)", usuario.getId());

		mvc.perform(get("/api/v1/me").with(sesionDe("google-123", "melina@example.org", "Melina")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.namespace").value("melina"));
	}

	private static OidcLoginRequestPostProcessor sesionDe(String sub, String email, String nombre) {
		return oidcLogin().idToken(token -> token.subject(sub).claim("email", email).claim("name", nombre));
	}
}
