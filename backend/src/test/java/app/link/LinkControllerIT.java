package app.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * Tests de integración de {@code POST /api/v1/links} contra el contrato docs/api/openapi.yaml
 * (HU-01, criterios CA-01.1 a CA-01.4).
 */
@SpringBootTest
class LinkControllerIT {

	private static final String ALIAS_PATTERN = "[23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz]{5}";

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup(context).build();
	}

	@Test
	void creaUnEnlaceYDevuelve201ConLocation() throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"https://example.org/una/ruta?x=1\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("http://short\\.test/" + ALIAS_PATTERN)))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.alias").value(matchesPattern(ALIAS_PATTERN)))
				.andExpect(jsonPath("$.originalUrl").value("https://example.org/una/ruta?x=1"))
				.andReturn();

		String body = result.getResponse().getContentAsString();
		String alias = JsonPath.read(body, "$.alias");
		String shortUrl = JsonPath.read(body, "$.shortUrl");
		Instant createdAt = Instant.parse(JsonPath.read(body, "$.createdAt"));
		Instant expiresAt = Instant.parse(JsonPath.read(body, "$.expiresAt"));

		assertThat(shortUrl).isEqualTo("http://short.test/" + alias);
		assertThat(result.getResponse().getHeader("Location")).isEqualTo(shortUrl);
		assertThat(Duration.between(createdAt, expiresAt)).isEqualTo(Duration.ofMinutes(60));
	}

	@Test
	void laMismaUrlDosVecesDaAliasDistintos() throws Exception {
		String first = createAndGetAlias("https://example.org/misma");
		String second = createAndGetAlias("https://example.org/misma");

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void rechazaEsquemaNoPermitidoCon400ProblemDetail() throws Exception {
		mvc.perform(post("/api/v1/links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"ftp://example.org/archivo\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("La URL debe ser absoluta y usar el esquema http o https."));
	}

	@Test
	void rechazaUrlDelPropioServicio() throws Exception {
		mvc.perform(post("/api/v1/links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"http://short.test/abcde\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("No se puede acortar una URL del propio servicio."));
	}

	@Test
	void rechazaUrlVacia() throws Exception {
		mvc.perform(post("/api/v1/links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void rechazaUrlDeMasDe2048Caracteres() throws Exception {
		String url = "https://example.org/" + "a".repeat(2030);

		mvc.perform(post("/api/v1/links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"" + url + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void rechazaJsonMalFormado() throws Exception {
		mvc.perform(post("/api/v1/links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": "))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private String createAndGetAlias(String url) throws Exception {
		String body = mvc.perform(post("/api/v1/links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"" + url + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.alias");
	}
}
