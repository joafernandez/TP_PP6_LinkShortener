package app.espacio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import app.usuario.UsuarioService;

/** {@code PUT /api/v1/me/namespace} con la seguridad real y el login de Google simulado (ADR-0031). */
@SpringBootTest
class EspacioControllerIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	private Cookie xsrf;

	@BeforeEach
	void setUp() throws Exception {
		mvc = webAppContextSetup(context).apply(springSecurity()).build();
		jdbc.update("DELETE FROM namespace");
		jdbc.update("DELETE FROM app_user");
		usuarioService.registrarSiEsNuevo("google-joana", "joana@example.org", "Joana");
		usuarioService.registrarSiEsNuevo("google-ana", "ana@example.org", "Ana");
		// igual que la web: la cookie XSRF-TOKEN se reenvía en el encabezado X-XSRF-TOKEN
		xsrf = mvc.perform(get("/")).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}

	@Test
	void eligeElEspacioEnMinusculasYLoMuestraEnElPerfil() throws Exception {
		elegir("google-joana", "Joana23")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.namespace").value("joana23"));

		mvc.perform(get("/api/v1/me").with(sesion("google-joana")))
				.andExpect(jsonPath("$.namespace").value("joana23"));
	}

	@Test
	void dosUsuariosNoPuedenTenerElMismoEspacio() throws Exception {
		elegir("google-joana", "joana").andExpect(status().isOk());

		elegir("google-ana", "JOANA")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("El espacio 'joana' ya está en uso. Elegí otro."));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace", Integer.class)).isEqualTo(1);
	}

	@Test
	void alCambiarloElAnteriorQuedaLibreParaOtroUsuario() throws Exception {
		elegir("google-joana", "joana").andExpect(status().isOk());

		elegir("google-joana", "joana23")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.namespace").value("joana23"));
		elegir("google-ana", "joana")
				.andExpect(status().isOk());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace", Integer.class)).isEqualTo(2);
	}

	@Test
	void unNombreInvalidoSeRechazaConUnMensajeClaro() throws Exception {
		elegir("google-joana", "joana-23")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						"El espacio solo puede tener letras sin acentos y números, sin espacios ni símbolos."));
		elegir("google-joana", "abcdefghijklmnop")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El espacio puede tener hasta 15 caracteres."));
		elegir("google-joana", "api")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Ese nombre está reservado por el sistema. Elegí otro."));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace", Integer.class)).isZero();
	}

	@Test
	void sinSesionResponde401() throws Exception {
		mvc.perform(put("/api/v1/me/namespace").cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content("{\"namespace\": \"joana\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinTokenCsrfResponde403() throws Exception {
		mvc.perform(put("/api/v1/me/namespace").with(sesion("google-joana"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"namespace\": \"joana\"}"))
				.andExpect(status().isForbidden());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace", Integer.class)).isZero();
	}

	private ResultActions elegir(String googleSub, String nombre) throws Exception {
		return mvc.perform(put("/api/v1/me/namespace").with(sesion(googleSub))
				.cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON).content("{\"namespace\": \"" + nombre + "\"}"));
	}

	private static OidcLoginRequestPostProcessor sesion(String googleSub) {
		return oidcLogin().idToken(token -> token.subject(googleSub));
	}
}
