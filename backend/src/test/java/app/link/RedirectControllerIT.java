package app.link;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import app.support.MutableClock;
import app.support.TestClockConfig;

/**
 * Tests de integración de la redirección {@code GET /{alias}} (HU-04, criterios CA-04.1 a CA-04.4).
 */
@SpringBootTest
@Import(TestClockConfig.class)
class RedirectControllerIT {

	private static final Duration TTL = Duration.ofMinutes(60);

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private LinkService linkService;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup(context).build();
		jdbc.update("DELETE FROM link");
		clock.set(TestClockConfig.T0);
	}

	@Test
	void redirigeCon302ALaUrlOriginal() throws Exception {
		Link link = linkService.create("https://example.org/destino?x=1");

		mvc.perform(get("/" + link.getAlias()))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.org/destino?x=1"));
	}

	@Test
	void redirigeHastaUnInstanteAntesDeVencer() throws Exception {
		Link link = linkService.create("https://example.org/destino");

		clock.advance(TTL.minusMillis(1));

		mvc.perform(get("/" + link.getAlias()))
				.andExpect(status().isFound());
	}

	@Test
	void enlaceVencidoMuestraPaginaHtml404() throws Exception {
		Link link = linkService.create("https://example.org/destino");

		clock.advance(TTL);

		assertNotFoundPage("/" + link.getAlias());
	}

	@Test
	void aliasInexistenteMuestraLaMismaPaginaHtml404() throws Exception {
		assertNotFoundPage("/ZZZZZ");
	}

	@Test
	void palabraReservadaNoRedirigeYMuestraLaPagina404() throws Exception {
		assertNotFoundPage("/api");
	}

	@Test
	void swaggerUiSigueDisponible() throws Exception {
		mvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
	}

	@Test
	void especificacionOpenApiSigueDisponible() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void laApiSigueRespondiendoErroresEnJson() throws Exception {
		mvc.perform(get("/api/v1/links"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private void assertNotFoundPage(String path) throws Exception {
		mvc.perform(get(path))
				.andExpect(status().isNotFound())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(containsString("Enlace no disponible")));
	}
}
