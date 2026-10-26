package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.time.Duration;
import java.time.Instant;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import app.espacio.Espacio;
import app.espacio.EspacioRepository;
import app.support.MutableClock;
import app.support.TestClockConfig;
import app.usuario.UsuarioService;

/**
 * Enlaces dentro del espacio de un usuario (paso 2.4, ADR-0032), por HTTP con la seguridad real
 * y el login de Google simulado. Un test por regla del requerimiento.
 */
@SpringBootTest
@Import(TestClockConfig.class)
class EnlacesDeEspacioIT {

	private static final Duration TTL = Duration.ofMinutes(60);

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EnlaceDeEspacioRepository enlaceRepository;

	@Autowired
	private EspacioRepository espacioRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private MockMvc mvc;

	private Cookie xsrf;

	@BeforeEach
	void setUp() throws Exception {
		mvc = webAppContextSetup(context).apply(springSecurity()).build();
		clock.set(TestClockConfig.T0);
		jdbc.update("DELETE FROM namespace_link");
		jdbc.update("DELETE FROM namespace");
		jdbc.update("DELETE FROM app_user");
		jdbc.update("DELETE FROM link");
		usuarioService.registrarSiEsNuevo("google-joana", "joana@example.org", "Joana");
		usuarioService.registrarSiEsNuevo("google-ana", "ana@example.org", "Ana");
		usuarioService.registrarSiEsNuevo("google-sin-espacio", "nadie@example.org", "Nadie");
		xsrf = mvc.perform(get("/")).andReturn().getResponse().getCookie("XSRF-TOKEN");
		elegirEspacio("google-joana", "joana");
		elegirEspacio("google-ana", "ana");
	}

	@Test
	void sinCodigoSeAsignanCodigosIncrementalesDentroDelEspacio() throws Exception {
		acortar("google-joana", "https://example.org/uno", null)
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://short.test/joana/1"))
				.andExpect(jsonPath("$.alias").value("1"))
				.andExpect(jsonPath("$.shortUrl").value("http://short.test/joana/1"));
		assertThat(codigo(acortar("google-joana", "https://example.org/dos", ""))).isEqualTo("2");
		assertThat(codigo(acortar("google-joana", "https://example.org/tres", null))).isEqualTo("3");
	}

	@Test
	void elMismoCodigoPuedeUsarseEnEspaciosDistintos() throws Exception {
		assertThat(codigo(acortar("google-joana", "https://example.org/joana", null))).isEqualTo("1");
		assertThat(codigo(acortar("google-ana", "https://example.org/ana", null))).isEqualTo("1");

		redirige("/joana/1", "https://example.org/joana");
		redirige("/ana/1", "https://example.org/ana");
	}

	@Test
	void losEnlacesDeEspacioNoAfectanAlCaminoAnonimo() throws Exception {
		acortar("google-joana", "https://example.org/espacio", null).andExpect(status().isCreated());

		mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\": \"https://example.org/anonimo\"}"))
				.andExpect(jsonPath("$.alias").value("1"))
				.andExpect(jsonPath("$.shortUrl").value("http://short.test/1"));
		redirige("/1", "https://example.org/anonimo");
		redirige("/joana/1", "https://example.org/espacio");
	}

	@Test
	void aceptaCodigosPersonalizadosConGuionYGuionBajo() throws Exception {
		for (String codigo : new String[] { "final", "final-2026", "tp_java" }) {
			acortar("google-joana", "https://example.org/" + codigo, codigo)
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.shortUrl").value("http://short.test/joana/" + codigo));
			redirige("/joana/" + codigo, "https://example.org/" + codigo);
		}
	}

	@Test
	void elCodigoAutomaticoSalteaLosPersonalizadosVigentes() throws Exception {
		acortar("google-joana", "https://example.org/personalizado", "1").andExpect(status().isCreated());

		assertThat(codigo(acortar("google-joana", "https://example.org/automatico", null))).isEqualTo("2");
	}

	@Test
	void unCodigoPersonalizadoOcupadoSeRechazaConUnMensajeClaro() throws Exception {
		acortar("google-joana", "https://example.org/primero", "final").andExpect(status().isCreated());

		acortar("google-joana", "https://example.org/segundo", "FINAL")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("El código 'final' ya está en uso en tu espacio. Elegí otro."));
		redirige("/joana/final", "https://example.org/primero");
	}

	@Test
	void unCodigoPersonalizadoVencidoVuelveAEstarDisponibleYLlevaSoloALaUrlNueva() throws Exception {
		acortar("google-joana", "https://example.org/vieja", "final").andExpect(status().isCreated());
		clock.advance(TTL);

		acortar("google-joana", "https://example.org/nueva", "final").andExpect(status().isCreated());

		redirige("/joana/final", "https://example.org/nueva");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace_link", Integer.class)).isEqualTo(1);
	}

	@Test
	void unCodigoInvalidoSeRechazaSinCrearElEnlace() throws Exception {
		acortar("google-joana", "https://example.org", "mi codigo")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						"El código solo puede tener letras sin acentos, números, guion (-) y guion bajo (_), sin espacios."));
		acortar("google-joana", "https://example.org", "abcdefghijklmnop")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("El código puede tener hasta 15 caracteres."));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace_link", Integer.class)).isZero();
	}

	@Test
	void losEnlacesVencenALos60Minutos() throws Exception {
		acortar("google-joana", "https://example.org/destino", null).andExpect(status().isCreated());

		clock.advance(TTL.minusSeconds(1));
		redirige("/joana/1", "https://example.org/destino");

		clock.advance(Duration.ofSeconds(1));
		mvc.perform(get("/joana/1"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}

	@Test
	void elEspacioYElCodigoSeTratanEnMinusculasAlAbrirElEnlace() throws Exception {
		acortar("google-joana", "https://example.org/destino", "Final").andExpect(status().isCreated());

		redirige("/Joana/FINAL", "https://example.org/destino");
	}

	@Test
	void alCambiarDeEspacioLosEnlacesDelAnteriorDejanDeFuncionarYNoPasanAlNuevo() throws Exception {
		acortar("google-joana", "https://example.org/uno", null).andExpect(status().isCreated());
		acortar("google-joana", "https://example.org/final", "final").andExpect(status().isCreated());

		elegirEspacio("google-joana", "joana23");

		mvc.perform(get("/joana/1")).andExpect(status().isNotFound());
		mvc.perform(get("/joana23/1")).andExpect(status().isNotFound());
		mvc.perform(get("/joana23/final")).andExpect(status().isNotFound());
		// en el espacio nuevo los códigos vuelven a estar libres
		assertThat(codigo(acortar("google-joana", "https://example.org/nuevo", null))).isEqualTo("1");
		redirige("/joana23/1", "https://example.org/nuevo");
	}

	@Test
	void otroUsuarioQueTomaElEspacioLiberadoNoHeredaLosEnlaces() throws Exception {
		acortar("google-joana", "https://example.org/de-joana", null).andExpect(status().isCreated());
		elegirEspacio("google-joana", "joana23");

		elegirEspacio("google-sin-espacio", "joana");

		mvc.perform(get("/joana/1")).andExpect(status().isNotFound());
	}

	@Test
	void sinEspacioElegidoPideElegirloAntesDeAcortar() throws Exception {
		acortar("google-sin-espacio", "https://example.org", null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Elegí tu espacio antes de crear enlaces."));
	}

	@Test
	void sinSesionResponde401YSinTokenCsrf403() throws Exception {
		mvc.perform(post("/api/v1/me/links").cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"https://example.org\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/me/links").with(sesion("google-joana"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"https://example.org\"}"))
				.andExpect(status().isForbidden());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace_link", Integer.class)).isZero();
	}

	@Test
	void unaUrlInvalidaSeRechazaComoEnElCaminoAnonimo() throws Exception {
		acortar("google-joana", "ftp://example.org", null).andExpect(status().isBadRequest());
	}

	@Test
	void elQrDeUnEnlaceDeEspacioEsUnPngYDa404SiNoExiste() throws Exception {
		acortar("google-joana", "https://example.org/destino", null).andExpect(status().isCreated());

		mvc.perform(get("/api/v1/namespaces/joana/links/1/qr"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(header().string("Cache-Control", "no-store"));
		mvc.perform(get("/api/v1/namespaces/joana/links/99/qr"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void laBaseRechazaElMismoCodigoDosVecesEnUnEspacioYLoTraduceAColision() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		Instant ahora = TestClockConfig.T0;
		transaction.executeWithoutResult(status -> enlaceRepository.insertar(new EnlaceDeEspacio(
				espacioRepository.buscarPorNombre("joana").orElseThrow(), "x", "https://example.org/1", ahora,
				ahora.plus(TTL))));

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			Espacio joana = espacioRepository.buscarPorNombre("joana").orElseThrow();
			enlaceRepository.insertar(new EnlaceDeEspacio(joana, "x", "https://example.org/2", ahora, ahora.plus(TTL)));
		})).isInstanceOf(ColisionAliasException.class);
	}

	private void elegirEspacio(String googleSub, String nombre) throws Exception {
		mvc.perform(put("/api/v1/me/namespace").with(sesion(googleSub))
						.cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content("{\"namespace\": \"" + nombre + "\"}"))
				.andExpect(status().isOk());
	}

	private ResultActions acortar(String googleSub, String url, String codigo) throws Exception {
		String cuerpo = codigo == null
				? "{\"url\": \"" + url + "\"}"
				: "{\"url\": \"" + url + "\", \"code\": \"" + codigo + "\"}";
		return mvc.perform(post("/api/v1/me/links").with(sesion(googleSub))
				.cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private static String codigo(ResultActions resultado) throws Exception {
		return JsonPath.read(resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
				"$.alias");
	}

	private void redirige(String ruta, String destino) throws Exception {
		mvc.perform(get(ruta))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", destino));
	}

	private static OidcLoginRequestPostProcessor sesion(String googleSub) {
		return oidcLogin().idToken(token -> token.subject(googleSub));
	}
}
