package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import app.common.config.AppProperties;
import app.enlace.alias.GeneradorAlias;
import app.support.MutableClock;
import app.support.TestClockConfig;

/**
 * Tests de integración de {@link EnlaceService} sobre HSQLDB en memoria,
 * con reloj y generador de alias controlados.
 */
@SpringBootTest
@Import(TestClockConfig.class)
class EnlaceServiceIT {

	private static final Instant T0 = TestClockConfig.T0;
	private static final Duration TTL = Duration.ofMinutes(60);

	@Autowired
	private EnlaceService enlaceService;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private GeneradorAlias generadorAlias;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private AppProperties propiedades;

	@Autowired
	private ValidadorUrl validadorUrl;

	@BeforeEach
	void setUp() {
		jdbc.update("DELETE FROM link");
		clock.set(T0);
	}

	@Test
	void creaUnEnlaceVigentePor60Minutos() {
		when(generadorAlias.generar()).thenReturn("AAAAA");

		Enlace enlace = enlaceService.crear("https://example.org/uno");

		assertThat(enlace.getAlias()).isEqualTo("AAAAA");
		assertThat(enlace.getUrlOriginal()).isEqualTo("https://example.org/uno");
		assertThat(enlace.getCreadoEn()).isEqualTo(T0);
		assertThat(enlace.getVenceEn()).isEqualTo(T0.plus(TTL));
		assertThat(filasConAlias("AAAAA")).isEqualTo(1);
	}

	@Test
	void armaLaUrlCortaConLaUrlBase() {
		when(generadorAlias.generar()).thenReturn("AAAAA");

		Enlace enlace = enlaceService.crear("https://example.org/uno");

		assertThat(enlaceService.urlCortaDe(enlace)).isEqualTo("http://short.test/AAAAA");
	}

	@Test
	void noPisaUnAliasVigenteYGeneraOtro() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");

		clock.advance(Duration.ofMinutes(10));
		when(generadorAlias.generar()).thenReturn("AAAAA", "BBBBB");
		Enlace second = enlaceService.crear("https://example.org/dos");

		assertThat(second.getAlias()).isEqualTo("BBBBB");
		assertThat(urlOriginalDe("AAAAA")).isEqualTo("https://example.org/uno");
	}

	@Test
	void reasignaUnAliasVencido() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");

		clock.advance(TTL);
		Enlace reasignado = enlaceService.crear("https://example.org/dos");

		assertThat(reasignado.getAlias()).isEqualTo("AAAAA");
		assertThat(reasignado.getUrlOriginal()).isEqualTo("https://example.org/dos");
		assertThat(reasignado.getCreadoEn()).isEqualTo(T0.plus(TTL));
		assertThat(reasignado.getVenceEn()).isEqualTo(T0.plus(TTL).plus(TTL));
		assertThat(filasConAlias("AAAAA")).isEqualTo(1);
		assertThat(urlOriginalDe("AAAAA")).isEqualTo("https://example.org/dos");
	}

	@Test
	void unSegundoAntesDeVencerElAliasSigueOcupado() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");

		clock.advance(TTL.minusSeconds(1));
		when(generadorAlias.generar()).thenReturn("AAAAA", "BBBBB");
		Enlace second = enlaceService.crear("https://example.org/dos");

		assertThat(second.getAlias()).isEqualTo("BBBBB");
	}

	@Test
	void laMismaUrlGeneraAliasDistintos() {
		when(generadorAlias.generar()).thenReturn("AAAAA", "BBBBB");

		Enlace first = enlaceService.crear("https://example.org/misma");
		Enlace second = enlaceService.crear("https://example.org/misma");

		assertThat(first.getAlias()).isNotEqualTo(second.getAlias());
	}

	@Test
	void fallaSiSeAgotanLosIntentos() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");

		assertThatThrownBy(() -> enlaceService.crear("https://example.org/dos"))
				.isInstanceOf(AliasNoDisponibleException.class);
		assertThat(urlOriginalDe("AAAAA")).isEqualTo("https://example.org/uno");
	}

	@Test
	void rechazaUrlInvalidaSinGenerarAlias() {
		assertThatThrownBy(() -> enlaceService.crear("ftp://example.org"))
				.isInstanceOf(UrlInvalidaException.class);
		verify(generadorAlias, never()).generar();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link", Integer.class)).isZero();
	}

	@Test
	@SuppressWarnings("unchecked")
	void unaColisionRealEnHsqldbPermiteCrearConOtroAlias() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");
		when(generadorAlias.generar()).thenReturn("AAAAA", "BBBBB");

		// Simula que la primera lectura no vio el alias que otro pedido ya insertó.
		// Solo se controla esa lectura: persist, flush, rollback y commit son reales.
		TypedQuery<Enlace> consultaDesactualizada = mock(TypedQuery.class);
		when(consultaDesactualizada.setParameter("alias", "AAAAA")).thenReturn(consultaDesactualizada);
		when(consultaDesactualizada.getResultStream()).thenAnswer(invocation -> Stream.empty());
		EntityManager entityManagerConcurrente = mock(EntityManager.class, delegatesTo(entityManager));
		doReturn(consultaDesactualizada).doAnswer(invocation -> entityManager.createQuery(
				invocation.getArgument(0, String.class), Enlace.class))
				.when(entityManagerConcurrente).createQuery("SELECT l FROM Enlace l WHERE l.alias = :alias", Enlace.class);
		AtomicReference<ConstraintViolationException> collision = new AtomicReference<>();
		doAnswer(invocation -> {
			try {
				// IDENTITY ejecuta la inserción durante persist, antes del flush explícito.
				entityManager.persist(invocation.getArgument(0, Enlace.class));
			} catch (ConstraintViolationException error) {
				collision.set(error);
				throw error;
			}
			return null;
		}).when(entityManagerConcurrente).persist(any(Enlace.class));
		EnlaceService serviceConcurrente = new EnlaceService(entityManagerConcurrente, transactionManager,
				generadorAlias, validadorUrl, propiedades, clock);

		Enlace creado = serviceConcurrente.crear("https://example.org/dos");

		assertThat(collision.get()).isNotNull();
		assertThat(collision.get().getSQLState()).isEqualTo("23505");
		assertThat(collision.get().getConstraintName()).matches("(?i)(PUBLIC\\.)?UK_LINK_ALIAS");
		assertThat(creado.getAlias()).isEqualTo("BBBBB");
		assertThat(urlOriginalDe("AAAAA")).isEqualTo("https://example.org/uno");
		assertThat(urlOriginalDe("BBBBB")).isEqualTo("https://example.org/dos");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link", Integer.class)).isEqualTo(2);
	}

	@Test
	void unAliasDemasiadoLargoPropagaElErrorDeLaBaseSinReintentar() {
		when(generadorAlias.generar()).thenReturn("A".repeat(17));

		assertThatThrownBy(() -> enlaceService.crear("https://example.org/uno"))
				.isInstanceOf(PersistenceException.class)
				.isNotInstanceOf(AliasNoDisponibleException.class);
		verify(generadorAlias).generar();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link", Integer.class)).isZero();
	}

	@Test
	void resolverDevuelveElEnlaceVigente() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");

		clock.advance(TTL.minusSeconds(1));
		Enlace enlace = enlaceService.resolver("AAAAA");

		assertThat(enlace.getUrlOriginal()).isEqualTo("https://example.org/uno");
	}

	@Test
	void resolverFallaExactamenteALos60Minutos() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");

		clock.advance(TTL);

		assertThatThrownBy(() -> enlaceService.resolver("AAAAA"))
				.isInstanceOf(EnlaceNoEncontradoException.class);
	}

	@Test
	void resolverFallaSiElAliasNoExiste() {
		assertThatThrownBy(() -> enlaceService.resolver("ZZZZZ"))
				.isInstanceOf(EnlaceNoEncontradoException.class);
	}

	@Test
	void resolverDevuelveLaNuevaUrlTrasUnaReasignacion() {
		when(generadorAlias.generar()).thenReturn("AAAAA");
		enlaceService.crear("https://example.org/uno");
		clock.advance(TTL);
		enlaceService.crear("https://example.org/dos");

		Enlace enlace = enlaceService.resolver("AAAAA");

		assertThat(enlace.getUrlOriginal()).isEqualTo("https://example.org/dos");
	}

	private int filasConAlias(String alias) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM link WHERE alias = ?", Integer.class, alias);
	}

	private String urlOriginalDe(String alias) {
		return jdbc.queryForObject("SELECT original_url FROM link WHERE alias = ?", String.class, alias);
	}
}
