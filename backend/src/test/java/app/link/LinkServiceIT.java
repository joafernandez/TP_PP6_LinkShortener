package app.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import app.link.alias.AliasGenerator;
import app.support.MutableClock;
import app.support.TestClockConfig;

/**
 * Tests de integración de {@link LinkService} sobre HSQLDB en memoria,
 * con reloj y generador de alias controlados.
 */
@SpringBootTest
@Import(TestClockConfig.class)
class LinkServiceIT {

	private static final Instant T0 = TestClockConfig.T0;
	private static final Duration TTL = Duration.ofMinutes(60);

	@Autowired
	private LinkService linkService;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private AliasGenerator aliasGenerator;

	@BeforeEach
	void setUp() {
		jdbc.update("DELETE FROM link");
		clock.set(T0);
	}

	@Test
	void creaUnEnlaceVigentePor60Minutos() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");

		Link link = linkService.create("https://example.org/uno");

		assertThat(link.getAlias()).isEqualTo("AAAAA");
		assertThat(link.getOriginalUrl()).isEqualTo("https://example.org/uno");
		assertThat(link.getCreatedAt()).isEqualTo(T0);
		assertThat(link.getExpiresAt()).isEqualTo(T0.plus(TTL));
		assertThat(rowsWithAlias("AAAAA")).isEqualTo(1);
	}

	@Test
	void armaLaUrlCortaConLaUrlBase() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");

		Link link = linkService.create("https://example.org/uno");

		assertThat(linkService.shortUrlOf(link)).isEqualTo("http://short.test/AAAAA");
	}

	@Test
	void noPisaUnAliasVigenteYGeneraOtro() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");
		linkService.create("https://example.org/uno");

		clock.advance(Duration.ofMinutes(10));
		when(aliasGenerator.generate()).thenReturn("AAAAA", "BBBBB");
		Link second = linkService.create("https://example.org/dos");

		assertThat(second.getAlias()).isEqualTo("BBBBB");
		assertThat(originalUrlOf("AAAAA")).isEqualTo("https://example.org/uno");
	}

	@Test
	void reasignaUnAliasVencido() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");
		linkService.create("https://example.org/uno");

		clock.advance(TTL);
		Link reassigned = linkService.create("https://example.org/dos");

		assertThat(reassigned.getAlias()).isEqualTo("AAAAA");
		assertThat(reassigned.getOriginalUrl()).isEqualTo("https://example.org/dos");
		assertThat(reassigned.getCreatedAt()).isEqualTo(T0.plus(TTL));
		assertThat(reassigned.getExpiresAt()).isEqualTo(T0.plus(TTL).plus(TTL));
		assertThat(rowsWithAlias("AAAAA")).isEqualTo(1);
		assertThat(originalUrlOf("AAAAA")).isEqualTo("https://example.org/dos");
	}

	@Test
	void unSegundoAntesDeVencerElAliasSigueOcupado() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");
		linkService.create("https://example.org/uno");

		clock.advance(TTL.minusSeconds(1));
		when(aliasGenerator.generate()).thenReturn("AAAAA", "BBBBB");
		Link second = linkService.create("https://example.org/dos");

		assertThat(second.getAlias()).isEqualTo("BBBBB");
	}

	@Test
	void laMismaUrlGeneraAliasDistintos() {
		when(aliasGenerator.generate()).thenReturn("AAAAA", "BBBBB");

		Link first = linkService.create("https://example.org/misma");
		Link second = linkService.create("https://example.org/misma");

		assertThat(first.getAlias()).isNotEqualTo(second.getAlias());
	}

	@Test
	void fallaSiSeAgotanLosIntentos() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");
		linkService.create("https://example.org/uno");

		assertThatThrownBy(() -> linkService.create("https://example.org/dos"))
				.isInstanceOf(AliasUnavailableException.class);
		assertThat(originalUrlOf("AAAAA")).isEqualTo("https://example.org/uno");
	}

	@Test
	void rechazaUrlInvalidaSinGenerarAlias() {
		assertThatThrownBy(() -> linkService.create("ftp://example.org"))
				.isInstanceOf(InvalidUrlException.class);
		verify(aliasGenerator, never()).generate();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link", Integer.class)).isZero();
	}

	@Test
	void resolveDevuelveElEnlaceVigente() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");
		linkService.create("https://example.org/uno");

		clock.advance(TTL.minusSeconds(1));
		Link link = linkService.resolve("AAAAA");

		assertThat(link.getOriginalUrl()).isEqualTo("https://example.org/uno");
	}

	@Test
	void resolveFallaExactamenteALos60Minutos() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");
		linkService.create("https://example.org/uno");

		clock.advance(TTL);

		assertThatThrownBy(() -> linkService.resolve("AAAAA"))
				.isInstanceOf(LinkNotFoundException.class);
	}

	@Test
	void resolveFallaSiElAliasNoExiste() {
		assertThatThrownBy(() -> linkService.resolve("ZZZZZ"))
				.isInstanceOf(LinkNotFoundException.class);
	}

	@Test
	void resolveDevuelveLaNuevaUrlTrasUnaReasignacion() {
		when(aliasGenerator.generate()).thenReturn("AAAAA");
		linkService.create("https://example.org/uno");
		clock.advance(TTL);
		linkService.create("https://example.org/dos");

		Link link = linkService.resolve("AAAAA");

		assertThat(link.getOriginalUrl()).isEqualTo("https://example.org/dos");
	}

	private int rowsWithAlias(String alias) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM link WHERE alias = ?", Integer.class, alias);
	}

	private String originalUrlOf(String alias) {
		return jdbc.queryForObject("SELECT original_url FROM link WHERE alias = ?", String.class, alias);
	}
}
