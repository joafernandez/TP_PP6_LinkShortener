package app.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import app.enlace.Enlace;
import app.enlace.EnlaceService;
import app.enlace.alias.GeneradorAlias;
import app.support.MutableClock;
import app.support.TestClockConfig;

/** Verifica el contrato del QR sobre HSQLDB en memoria (HU-06). */
@SpringBootTest
@Import(TestClockConfig.class)
class QrControllerIT {

	private static final Duration TTL = Duration.ofMinutes(60);
	private static final String QR_PATH = "/api/v1/links/AAAAA/qr";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private EnlaceService enlaceService;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private GeneradorAlias generadorAlias;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup(context).build();
		jdbc.update("DELETE FROM link");
		clock.set(TestClockConfig.T0);
		when(generadorAlias.generar()).thenReturn("AAAAA");
	}

	@Test
	void devuelveUnPngQueCodificaLaUrlCorta() throws Exception {
		Enlace enlace = enlaceService.crear("https://example.org/destino?x=1");

		byte[] png = requestPng();

		assertThat(decode(png)).isEqualTo(enlaceService.urlCortaDe(enlace));
	}

	@Test
	void devuelveElQrHastaUnInstanteAntesDeVencer() throws Exception {
		enlaceService.crear("https://example.org/destino");
		clock.advance(TTL.minusMillis(1));

		requestPng();
	}

	@Test
	void vencidoExactamenteALos60MinutosDevuelve404Json() throws Exception {
		enlaceService.crear("https://example.org/destino");
		clock.advance(TTL);

		assertNotFound();
	}

	@Test
	void aliasInexistenteDevuelveElMismo404Json() throws Exception {
		assertNotFound();
	}

	@Test
	void unAliasReasignadoVuelveATenerQrYRedirigeAlNuevoDestino() throws Exception {
		enlaceService.crear("https://example.org/uno");
		clock.advance(TTL);
		assertNotFound();
		Enlace reasignado = enlaceService.crear("https://example.org/dos");

		assertThat(decode(requestPng())).isEqualTo(enlaceService.urlCortaDe(reasignado));
		mvc.perform(get("/AAAAA"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.org/dos"));
	}

	@Test
	void pedirElQrNoCreaEnlacesNiRenuevaLaVigencia() throws Exception {
		Enlace original = enlaceService.crear("https://example.org/destino");
		clock.advance(Duration.ofMinutes(30));

		requestPng();

		Enlace afterRequest = enlaceService.resolver("AAAAA");
		assertThat(afterRequest.getCreadoEn()).isEqualTo(original.getCreadoEn());
		assertThat(afterRequest.getVenceEn()).isEqualTo(original.getVenceEn());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link", Integer.class)).isEqualTo(1);
		clock.advance(Duration.ofMinutes(30));
		assertNotFound();
	}

	@Test
	void swaggerDocumentaPng404JsonYCacheComoElContrato() throws Exception {
		String operation = "$.paths['/api/v1/links/{alias}/qr'].get";
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(operation + ".operationId").value("getLinkQr"))
				.andExpect(jsonPath(operation + ".tags[0]").value("QR"))
				.andExpect(jsonPath(operation + ".responses['200'].content['image/png'].schema.type")
						.value("string"))
				.andExpect(jsonPath(operation + ".responses['200'].content['image/png'].schema.contentMediaType")
						.value("image/png"))
				.andExpect(jsonPath(operation + ".responses['404'].content['application/problem+json']").exists())
				.andExpect(jsonPath(operation + ".responses['200'].headers['Cache-Control'].schema.enum[0]")
						.value("no-store"))
				.andExpect(jsonPath(operation + ".responses['404'].headers['Cache-Control'].schema.enum[0]")
						.value("no-store"))
				.andExpect(jsonPath("$.components.schemas.ProblemDetail").exists())
				.andExpect(jsonPath("$.components.schemas.CreateLinkRequest.properties.url").exists())
				.andExpect(jsonPath("$.components.schemas.LinkResponse.properties.length()").value(5))
				.andExpect(jsonPath("$.components.schemas.LinkResponse.properties.shortUrl").exists())
				.andExpect(jsonPath("$.components.schemas.LinkResponse.properties.originalUrl").exists())
				.andExpect(jsonPath("$.components.schemas.LinkResponse.properties.createdAt").exists())
				.andExpect(jsonPath("$.components.schemas.LinkResponse.properties.expiresAt").exists())
				.andExpect(jsonPath("$.components.schemas.EnlaceResponse").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/links'].post.operationId").value("createLink"));
	}

	private byte[] requestPng() throws Exception {
		MvcResult result = mvc.perform(get(QR_PATH).accept(MediaType.IMAGE_PNG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andReturn();
		return result.getResponse().getContentAsByteArray();
	}

	private void assertNotFound() throws Exception {
		mvc.perform(get(QR_PATH).accept(MediaType.IMAGE_PNG))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("El enlace no existe o está vencido."))
				.andExpect(jsonPath("$.instance").value(QR_PATH));
	}

	private static String decode(byte[] png) throws Exception {
		var image = ImageIO.read(new ByteArrayInputStream(png));
		assertThat(image).isNotNull();
		assertThat(image.getWidth()).isEqualTo(300);
		assertThat(image.getHeight()).isEqualTo(300);
		BinaryBitmap bitmap = new BinaryBitmap(
				new HybridBinarizer(new BufferedImageLuminanceSource(image)));
		return new MultiFormatReader().decode(bitmap, Map.of(DecodeHintType.PURE_BARCODE, true)).getText();
	}
}
