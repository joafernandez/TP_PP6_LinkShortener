package app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/** Verifica que la web empaquetada sea accesible sin confundirse con la ruta del alias (CA-04.4). */
@SpringBootTest
class WebResourcesIT {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup(context).build();
	}

	@Test
	void laRaizSirveLaPaginaDeInicio() throws Exception {
		// MockMvc comprueba el forward; el contenido final se verifica por HTTP en el navegador.
		mvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html"));
	}

	@Test
	void elFormularioYLosRecursosEstaticosEstanDisponibles() throws Exception {
		String html = mvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(html).contains("Dirección a acortar", "/assets/app.js", "/assets/styles.css");
		mvc.perform(get("/assets/styles.css"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/css"));
		mvc.perform(get("/assets/app.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/api/v1/links")));
	}
}
