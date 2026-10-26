package app.qrlogin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import app.support.MutableClock;
import app.support.TestClockConfig;
import app.usuario.UsuarioService;

/** Login con QR de punta a punta con la seguridad real (ADR-0033): aprobado, rechazado, vencido y usado dos veces. */
@SpringBootTest
@Import(TestClockConfig.class)
class QrLoginIT {

	private static final String BASE = "/api/v1/auth/qr-logins";
	private static final String ENCABEZADO = "X-Qr-Secret";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private QrLoginService qrLoginService;

	@Autowired
	private MutableClock reloj;

	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup(context).apply(springSecurity()).build();
		reloj.set(TestClockConfig.T0);
		jdbc.update("DELETE FROM qr_login");
		jdbc.update("DELETE FROM namespace");
		jdbc.update("DELETE FROM app_user");
		usuarioService.registrarSiEsNuevo("google-123", "melina@example.org", "Melina");
	}

	@Test
	void crearDevuelveCodigoYSecretoSinSesionNiCsrf() throws Exception {
		mvc.perform(post(BASE))
				.andExpect(status().isCreated())
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
				.andExpect(jsonPath("$.code").isNotEmpty())
				.andExpect(jsonPath("$.secret").isNotEmpty())
				.andExpect(jsonPath("$.expiresAt").exists());
	}

	@Test
	void elPedidoRecienCreadoEstaPendiente() throws Exception {
		Pedido pedido = crear();
		estadoEs(pedido, "PENDING");
	}

	@Test
	void flujoAprobadoAbreLaSesionEnLaComputadoraYSoloUnaVez() throws Exception {
		Pedido pedido = crear();

		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()).with(celular()))
				.andExpect(status().isNoContent());
		estadoEs(pedido, "AUTHORIZED");

		MvcResult canje = mvc.perform(post(BASE + "/" + pedido.code + "/session").header(ENCABEZADO, pedido.secret))
				.andExpect(status().isNoContent())
				.andReturn();
		MockHttpSession sesion = (MockHttpSession) canje.getRequest().getSession(false);
		assertThat(sesion).isNotNull();
		mvc.perform(get("/api/v1/me").session(sesion))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("melina@example.org"));

		estadoEs(pedido, "USED");
		mvc.perform(post(BASE + "/" + pedido.code + "/session").header(ENCABEZADO, pedido.secret))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void conUnaSesionPreviaElCanjeCambiaElIdentificador() throws Exception {
		Pedido pedido = crear();
		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()).with(celular()))
				.andExpect(status().isNoContent());
		MockHttpSession previa = new MockHttpSession();
		String idPrevio = previa.getId();

		MvcResult canje = mvc.perform(post(BASE + "/" + pedido.code + "/session")
				.header(ENCABEZADO, pedido.secret).session(previa))
				.andExpect(status().isNoContent())
				.andReturn();

		assertThat(canje.getRequest().getSession(false).getId()).isNotEqualTo(idPrevio);
	}

	@Test
	void flujoRechazadoNoPermiteIniciarSesion() throws Exception {
		Pedido pedido = crear();

		mvc.perform(post(BASE + "/" + pedido.code + "/reject").with(csrf()).with(celular()))
				.andExpect(status().isNoContent());

		estadoEs(pedido, "REJECTED");
		mvc.perform(post(BASE + "/" + pedido.code + "/session").header(ENCABEZADO, pedido.secret))
				.andExpect(status().isConflict());
	}

	@Test
	void pedidoVencidoSeInformaComoVencidoYNoSeDejaAutorizar() throws Exception {
		Pedido pedido = crear();

		reloj.advance(Duration.ofMinutes(2));

		estadoEs(pedido, "EXPIRED");
		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()).with(celular()))
				.andExpect(status().isConflict());
		mvc.perform(post(BASE + "/" + pedido.code + "/session").header(ENCABEZADO, pedido.secret))
				.andExpect(status().isConflict());
	}

	@Test
	void unSegundoAntesDeVencerTodaviaSePuedeAutorizar() throws Exception {
		Pedido pedido = crear();

		reloj.advance(Duration.ofMinutes(2).minusSeconds(1));

		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()).with(celular()))
				.andExpect(status().isNoContent());
	}

	@Test
	void pedidoAutorizadoQueVenceAntesDelCanjeNoIniciaSesion() throws Exception {
		Pedido pedido = crear();
		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()).with(celular()))
				.andExpect(status().isNoContent());

		reloj.advance(Duration.ofMinutes(3));

		estadoEs(pedido, "EXPIRED");
		mvc.perform(post(BASE + "/" + pedido.code + "/session").header(ENCABEZADO, pedido.secret))
				.andExpect(status().isConflict());
	}

	@Test
	void noSePuedeCanjearUnPedidoPendiente() throws Exception {
		Pedido pedido = crear();

		mvc.perform(post(BASE + "/" + pedido.code + "/session").header(ENCABEZADO, pedido.secret))
				.andExpect(status().isConflict());
	}

	@Test
	void unPedidoYaRespondidoNoSePuedeResponderDeNuevo() throws Exception {
		Pedido pedido = crear();
		mvc.perform(post(BASE + "/" + pedido.code + "/reject").with(csrf()).with(celular()))
				.andExpect(status().isNoContent());

		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()).with(celular()))
				.andExpect(status().isConflict());
	}

	@Test
	void conUnSecretoIncorrectoNoSeVeElEstadoNiSeCanjea() throws Exception {
		Pedido pedido = crear();
		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()).with(celular()))
				.andExpect(status().isNoContent());

		mvc.perform(get(BASE + "/" + pedido.code).header(ENCABEZADO, "otro-secreto"))
				.andExpect(status().isNotFound());
		mvc.perform(post(BASE + "/" + pedido.code + "/session").header(ENCABEZADO, "otro-secreto"))
				.andExpect(status().isNotFound());
		estadoEs(pedido, "AUTHORIZED");
	}

	@Test
	void sinElSecretoLaConsultaEsUnaSolicitudIncorrecta() throws Exception {
		Pedido pedido = crear();

		mvc.perform(get(BASE + "/" + pedido.code)).andExpect(status().isBadRequest());
	}

	@Test
	void unCodigoInexistenteDaNoEncontrado() throws Exception {
		mvc.perform(post(BASE + "/inexistente/authorize").with(csrf()).with(celular()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		mvc.perform(get(BASE + "/inexistente").header(ENCABEZADO, "x")).andExpect(status().isNotFound());
	}

	@Test
	void autorizarSinSesionDelCelularDa401() throws Exception {
		Pedido pedido = crear();

		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf()))
				.andExpect(status().isUnauthorized());
		estadoEs(pedido, "PENDING");
	}

	@Test
	void autorizarSinTokenCsrfEsRechazado() throws Exception {
		Pedido pedido = crear();

		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(celular()))
				.andExpect(status().isForbidden());
		mvc.perform(post(BASE + "/" + pedido.code + "/reject").with(celular()))
				.andExpect(status().isForbidden());
		estadoEs(pedido, "PENDING");
	}

	@Test
	void unUsuarioNoRegistradoNoPuedeAutorizar() throws Exception {
		Pedido pedido = crear();

		mvc.perform(post(BASE + "/" + pedido.code + "/authorize").with(csrf())
				.with(oidcLogin().idToken(token -> token.subject("desconocido"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void laImagenQrCodificaLaPaginaDelCelular() throws Exception {
		Pedido pedido = crear();

		byte[] png = mvc.perform(get(BASE + "/" + pedido.code + "/qr"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
				.andReturn().getResponse().getContentAsByteArray();

		assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
		assertThat(qrLoginService.urlParaElCelular(pedido.code)).endsWith("/qr-login.html?c=" + pedido.code);
	}

	@Test
	void unCodigoDemasiadoLargoNoGeneraQr() throws Exception {
		mvc.perform(get(BASE + "/" + "a".repeat(65) + "/qr")).andExpect(status().isNotFound());
	}

	@Test
	void crearBorraLosPedidosVencidosHaceMasDeUnaHora() throws Exception {
		crear();
		reloj.advance(Duration.ofHours(2));

		crear();

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM qr_login", Integer.class)).isEqualTo(1);
	}

	@Test
	void elServicioNoAceptaUnSecretoNulo() {
		QrLogin pedido = qrLoginService.crear();

		assertThatThrownBy(() -> qrLoginService.estado(pedido.getCodigo(), null))
				.isInstanceOf(QrLoginNoEncontradoException.class);
	}

	private Pedido crear() throws Exception {
		String json = mvc.perform(post(BASE)).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return new Pedido(JsonPath.read(json, "$.code"), JsonPath.read(json, "$.secret"));
	}

	private void estadoEs(Pedido pedido, String esperado) throws Exception {
		mvc.perform(get(BASE + "/" + pedido.code).header(ENCABEZADO, pedido.secret))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(esperado));
	}

	private static OidcLoginRequestPostProcessor celular() {
		return oidcLogin().idToken(token -> token.subject("google-123").claim("email", "melina@example.org"));
	}

	private record Pedido(String code, String secret) {
	}
}
