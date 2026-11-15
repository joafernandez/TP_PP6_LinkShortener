package app.espacio;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import app.support.MutableClock;
import app.support.TestClockConfig;
import app.usuario.UsuarioService;

/**
 * Cambio de espacio con transición de 60 minutos para el nombre anterior (ADR-0034), por HTTP con la seguridad
 * real y el login de Google simulado. Un test por regla del requerimiento.
 */
@SpringBootTest
@Import(TestClockConfig.class)
class TransicionDeEspacioIT {

	private static final Instant T0 = TestClockConfig.T0;
	private static final Duration TRANSICION = Duration.ofMinutes(60);

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private NombreDeEspacioRepository nombreRepository;

	@Autowired
	private EspacioRepository espacioRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	private Cookie xsrf;

	@BeforeEach
	void setUp() throws Exception {
		mvc = webAppContextSetup(context).apply(springSecurity()).build();
		clock.set(T0);
		jdbc.update("DELETE FROM namespace_link");
		jdbc.update("DELETE FROM namespace_name");
		jdbc.update("DELETE FROM namespace");
		jdbc.update("DELETE FROM app_user");
		usuarioService.registrarSiEsNuevo("google-sofia", "sofia@example.org", "Sofía");
		usuarioService.registrarSiEsNuevo("google-ana", "ana@example.org", "Ana");
		xsrf = mvc.perform(get("/")).andReturn().getResponse().getCookie("XSRF-TOKEN");
		elegir("google-sofia", "sofi").andExpect(status().isOk());
	}

	@Test
	void durantelaTransicionLosEnlacesFuncionanConAmbosNombresSinDuplicarse() throws Exception {
		acortar("https://example.org/uno", null);
		acortar("https://example.org/dos", null);
		acortar("https://example.org/final", "final");

		elegir("google-sofia", "sofia")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.namespace").value("sofia"))
				.andExpect(jsonPath("$.previousNamespaces[0].namespace").value("sofi"))
				.andExpect(jsonPath("$.previousNamespaces[0].expiresAt").value(T0.plus(TRANSICION).toString()));

		for (String codigo : new String[] { "1", "2", "final" }) {
			String destino = "https://example.org/" + (codigo.equals("1") ? "uno" : codigo.equals("2") ? "dos" : "final");
			redirige("/sofia/" + codigo, destino);
			redirige("/sofi/" + codigo, destino);
		}
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace_link", Integer.class)).isEqualTo(3);
	}

	@Test
	void losEnlacesNuevosUsanElNombrePrincipal() throws Exception {
		elegir("google-sofia", "sofia").andExpect(status().isOk());

		acortar("https://example.org/nuevo", null)
				.andExpect(jsonPath("$.shortUrl").value("http://short.test/sofia/1"));
	}

	@Test
	void alos60MinutosElNombreAnteriorDejaDeFuncionarYElNuevoSigue() throws Exception {
		elegir("google-sofia", "sofia").andExpect(status().isOk());
		clock.advance(Duration.ofMinutes(50));
		acortar("https://example.org/vigente", null);

		clock.advance(Duration.ofMinutes(10).minusSeconds(1));
		redirige("/sofi/1", "https://example.org/vigente");

		clock.advance(Duration.ofSeconds(1));
		mvc.perform(get("/sofi/1"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
		redirige("/sofia/1", "https://example.org/vigente");
	}

	@Test
	void elCambioNoRenuevaElVencimientoDeLosEnlaces() throws Exception {
		// 13:30 se crea el enlace, 14:00 se cambia el espacio, 14:30 vence el enlace con los dos nombres
		acortar("https://example.org/destino", null);
		clock.advance(Duration.ofMinutes(30));
		elegir("google-sofia", "sofia").andExpect(status().isOk());

		clock.advance(Duration.ofMinutes(30));

		mvc.perform(get("/sofi/1")).andExpect(status().isNotFound());
		mvc.perform(get("/sofia/1")).andExpect(status().isNotFound());
	}

	@Test
	void elNombreAnteriorQuedaReservadoYDespuesSeLibera() throws Exception {
		elegir("google-sofia", "sofia").andExpect(status().isOk());

		elegir("google-ana", "SOFI")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("El espacio 'sofi' ya está en uso. Elegí otro."));

		// el enlace se crea a la mitad de la transición, así sigue vigente cuando el nombre se libera
		clock.advance(Duration.ofMinutes(30));
		acortar("https://example.org/de-sofia", null);
		clock.advance(Duration.ofMinutes(30));
		elegir("google-ana", "sofi").andExpect(status().isOk());

		// el espacio de Ana no hereda los enlaces de Sofía
		mvc.perform(get("/sofi/1")).andExpect(status().isNotFound());
		redirige("/sofia/1", "https://example.org/de-sofia");
	}

	@Test
	void conVariosCambiosCadaNombreAnteriorTieneSuPropioVencimiento() throws Exception {
		elegir("google-sofia", "sofia").andExpect(status().isOk());
		clock.advance(Duration.ofMinutes(10));

		ResultActions segundoCambio = elegir("google-sofia", "sofi2");
		// el enlace se crea después, así sigue vigente mientras vencen los dos nombres anteriores
		clock.advance(Duration.ofMinutes(15));
		acortar("https://example.org/destino", null);

		segundoCambio
				.andExpect(jsonPath("$.previousNamespaces[0].namespace").value("sofi"))
				.andExpect(jsonPath("$.previousNamespaces[0].expiresAt").value(T0.plus(TRANSICION).toString()))
				.andExpect(jsonPath("$.previousNamespaces[1].namespace").value("sofia"))
				.andExpect(jsonPath("$.previousNamespaces[1].expiresAt")
						.value(T0.plus(Duration.ofMinutes(70)).toString()));

		redirige("/sofi/1", "https://example.org/destino");
		redirige("/sofia/1", "https://example.org/destino");
		clock.set(T0.plus(TRANSICION));
		mvc.perform(get("/sofi/1")).andExpect(status().isNotFound());
		redirige("/sofia/1", "https://example.org/destino");
		clock.set(T0.plus(Duration.ofMinutes(70)));
		mvc.perform(get("/sofia/1")).andExpect(status().isNotFound());
		redirige("/sofi2/1", "https://example.org/destino");
	}

	@Test
	void sePuedeVolverAUnNombrePropioReservadoYLosDemasConservanSuVencimiento() throws Exception {
		elegir("google-sofia", "sofia").andExpect(status().isOk());
		clock.advance(Duration.ofMinutes(10));
		elegir("google-sofia", "sofi2").andExpect(status().isOk());
		clock.advance(Duration.ofMinutes(10));

		elegir("google-sofia", "sofi")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.namespace").value("sofi"))
				.andExpect(jsonPath("$.previousNamespaces.length()").value(2))
				.andExpect(jsonPath("$.previousNamespaces[0].namespace").value("sofia"))
				.andExpect(jsonPath("$.previousNamespaces[0].expiresAt")
						.value(T0.plus(Duration.ofMinutes(70)).toString()))
				.andExpect(jsonPath("$.previousNamespaces[1].namespace").value("sofi2"))
				.andExpect(jsonPath("$.previousNamespaces[1].expiresAt")
						.value(T0.plus(Duration.ofMinutes(80)).toString()));

		// sofi vuelve a ser el principal y deja de vencer
		clock.set(T0.plus(Duration.ofMinutes(200)));
		acortar("https://example.org/mucho-despues", null);
		redirige("/sofi/1", "https://example.org/mucho-despues");
		mvc.perform(get("/sofia/1")).andExpect(status().isNotFound());
	}

	@Test
	void elPerfilMuestraLosNombresAnterioresConSuVencimientoYDesaparecenAlExpirar() throws Exception {
		elegir("google-sofia", "sofia").andExpect(status().isOk());

		mvc.perform(get("/api/v1/me").with(sesion("google-sofia")))
				.andExpect(jsonPath("$.namespace").value("sofia"))
				.andExpect(jsonPath("$.previousNamespaces[0].namespace").value("sofi"))
				.andExpect(jsonPath("$.previousNamespaces[0].expiresAt").value(T0.plus(TRANSICION).toString()));

		clock.advance(TRANSICION);
		mvc.perform(get("/api/v1/me").with(sesion("google-sofia")))
				.andExpect(jsonPath("$.previousNamespaces.length()").value(0));
	}

	@Test
	void elQrConElNombreAnteriorFuncionaDuranteLaTransicion() throws Exception {
		acortar("https://example.org/destino", null);
		elegir("google-sofia", "sofia").andExpect(status().isOk());

		mvc.perform(get("/api/v1/namespaces/sofi/links/1/qr"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG));

		clock.advance(Duration.ofMinutes(59));
		mvc.perform(get("/api/v1/namespaces/sofi/links/1/qr")).andExpect(status().isOk());
		clock.advance(Duration.ofMinutes(1));
		mvc.perform(get("/api/v1/namespaces/sofi/links/1/qr")).andExpect(status().isNotFound());
	}

	@Test
	void laBaseImpideRegistrarDosVecesElMismoNombre() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> nombreRepository.insertar(
				new NombreDeEspacio(espacioRepository.buscarPorNombre("sofi").orElseThrow(), "sofi"))))
				.isInstanceOf(EspacioOcupadoException.class);
		assertThatThrownBy(() -> jdbc.update("INSERT INTO namespace_name (namespace_id, name) "
				+ "SELECT id, 'sofi' FROM namespace WHERE name = 'sofi'"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private ResultActions elegir(String googleSub, String nombre) throws Exception {
		return mvc.perform(put("/api/v1/me/namespace").with(sesion(googleSub))
				.cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON).content("{\"namespace\": \"" + nombre + "\"}"));
	}

	private ResultActions acortar(String url, String codigo) throws Exception {
		String cuerpo = codigo == null
				? "{\"url\": \"" + url + "\"}"
				: "{\"url\": \"" + url + "\", \"code\": \"" + codigo + "\"}";
		return mvc.perform(post("/api/v1/me/links").with(sesion("google-sofia"))
				.cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated());
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
