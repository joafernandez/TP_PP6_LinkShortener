package app.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Con Spring Security activo, todo lo de la Etapa 1 sigue público, y el login con Google
 * redirige a Google o vuelve a la web con un mensaje cuando falla (ADR-0030).
 */
@SpringBootTest
class SeguridadIT {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup(context).apply(springSecurity()).build();
		jdbc.update("DELETE FROM link");
	}

	@Test
	void sinSesionSePuedeAcortarRedirigirYVerElQr() throws Exception {
		// sin token CSRF, como la extensión y la web de la Etapa 1
		mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"https://example.org/destino\"}"))
				.andExpect(status().isCreated());

		mvc.perform(get("/1"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.org/destino"));
		mvc.perform(get("/api/v1/links/1/qr"))
				.andExpect(status().isOk());
	}

	@Test
	void laWebYSwaggerSiguenPublicos() throws Exception {
		mvc.perform(get("/")).andExpect(status().isOk());
		mvc.perform(get("/assets/app.js")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

	@Test
	void iniciarSesionRedirigeAGoogle() throws Exception {
		mvc.perform(get("/oauth2/authorization/google"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", startsWith("https://accounts.google.com/")))
				.andExpect(header().string("Location", containsString("client_id=test-client-id")));
	}

	@Test
	void unLoginCanceladoEnGoogleVuelveALaWebConUnMensajeYSinSesion() throws Exception {
		// Google vuelve con error=access_denied cuando el usuario cancela
		mvc.perform(get("/login/oauth2/code/google").param("error", "access_denied").param("state", "x"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/?login=error"));
	}

	@Test
	void cerrarSesionExigeElTokenCsrf() throws Exception {
		mvc.perform(post("/logout").with(oidcLogin()))
				.andExpect(status().isForbidden());
	}

	@Test
	void cerrarSesionFuncionaConLaCookieXsrfReenviadaEnElEncabezado() throws Exception {
		// lo mismo que hace la web: lee la cookie XSRF-TOKEN y la reenvía en X-XSRF-TOKEN
		Cookie xsrf = mvc.perform(get("/")).andReturn().getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrf).isNotNull();

		mvc.perform(post("/logout").with(oidcLogin()).cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
				.andExpect(status().isNoContent());
	}
}
