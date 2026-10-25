package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import app.support.MutableClock;
import app.support.TestClockConfig;

/**
 * Camino 1 de la Etapa 2: códigos secuenciales para usuarios no logueados (ADR-0026).
 * Un test por criterio de aceptación, por HTTP y con el generador real.
 */
@SpringBootTest
@Import(TestClockConfig.class)
class CodigosSecuencialesIT {

	private static final String ALFABETO = "123456789abcdefghijkmnpqrstuvwxyz";
	private static final Duration TTL = Duration.ofMinutes(60);

	@Autowired
	private WebApplicationContext context;

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
	void ca1_unaUrlValidaRecibeUnCodigoDeUnCaracter() throws Exception {
		mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"https://example.org/uno\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://short.test/1"))
				.andExpect(jsonPath("$.alias").value("1"))
				.andExpect(jsonPath("$.shortUrl").value("http://short.test/1"));
	}

	@Test
	void ca2_urlsConsecutivasRecibenCodigosIncrementalesSinRepetirse() throws Exception {
		List<String> codigos = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			codigos.add(acortar("https://example.org/" + i));
		}

		assertThat(codigos).containsExactly("1", "2", "3", "4", "5");
	}

	@Test
	void ca3_elCodigoDeUnEnlaceExpiradoVuelveAQuedarDisponible() throws Exception {
		acortar("https://example.org/uno");
		clock.advance(Duration.ofMinutes(10));
		acortar("https://example.org/dos");
		acortar("https://example.org/tres");

		// el "1" vence; el "2" y el "3" siguen vigentes
		clock.advance(TTL.minusMinutes(10));

		assertThat(acortar("https://example.org/cuatro")).isEqualTo("1");
		assertThat(acortar("https://example.org/cinco")).isEqualTo("4");
	}

	@Test
	void ca4_alAgotarseLosDeUnCaracterContinuaConLosDeDos() throws Exception {
		for (int i = 0; i < ALFABETO.length(); i++) {
			assertThat(acortar("https://example.org/" + i)).isEqualTo(String.valueOf(ALFABETO.charAt(i)));
		}

		assertThat(acortar("https://example.org/siguiente")).isEqualTo("11");
		assertThat(acortar("https://example.org/otra")).isEqualTo("12");
	}

	@Test
	void ca5_unCodigoVigenteRedirigeALaUrlOriginal() throws Exception {
		String codigo = acortar("https://example.org/destino");
		clock.advance(TTL.minusSeconds(1));

		mvc.perform(get("/" + codigo))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.org/destino"));
	}

	@Test
	void ca6_unCodigoExpiradoDejaDeRedireccionar() throws Exception {
		String codigo = acortar("https://example.org/destino");
		clock.advance(TTL);

		mvc.perform(get("/" + codigo))
				.andExpect(status().isNotFound());
	}

	@Test
	void ca7_unCodigoReutilizadoRedirigeSoloALaNuevaUrl() throws Exception {
		String anterior = acortar("https://example.org/anterior");
		clock.advance(TTL);
		String reutilizado = acortar("https://example.org/nueva");

		assertThat(reutilizado).isEqualTo(anterior);
		mvc.perform(get("/" + reutilizado))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.org/nueva"));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link WHERE original_url = ?", Integer.class,
				"https://example.org/anterior")).isZero();
	}

	private String acortar(String url) throws Exception {
		String respuesta = mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"" + url + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.alias");
	}
}
