package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;
import app.common.config.AppProperties.EnlaceSettings;
import app.enlace.alias.GeneradorAlias;

/** Verifica decisiones del negocio y transacciones sin conocer JPA ni errores SQL. */
class EnlaceServiceTest {

	private static final Instant AHORA = Instant.parse("2026-10-05T13:00:00Z");
	private final EnlaceRepository repository = mock(EnlaceRepository.class);
	private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
	private final GeneradorAlias generadorAlias = mock(GeneradorAlias.class);
	private EnlaceService service;

	@BeforeEach
	void setUp() {
		when(repository.buscarPorAlias(any())).thenReturn(Optional.empty());
		when(transactions.getTransaction(any())).thenAnswer(invocation -> mock(TransactionStatus.class));
		when(generadorAlias.generar(any())).thenReturn("AAAAA", "BBBBB", "CCCCC");
		AppProperties propiedades = new AppProperties("http://short.test",
				new EnlaceSettings(Duration.ofMinutes(60)),
				new AliasSettings("ABC", 3, Set.of()));
		service = new EnlaceService(repository, transactions, generadorAlias, new ValidadorUrl(propiedades),
				propiedades, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void revierteLaColisionAntesDeAbrirOtroIntento() {
		TransactionStatus primero = mock(TransactionStatus.class);
		TransactionStatus segundo = mock(TransactionStatus.class);
		when(transactions.getTransaction(any())).thenReturn(primero, segundo);
		doThrow(new ColisionAliasException(new IllegalStateException("Alias ocupado")))
				.doNothing().when(repository).insertar(any(Enlace.class));

		Enlace enlace = service.crear("https://example.org");

		assertThat(enlace.getAlias()).isEqualTo("BBBBB");
		InOrder orden = inOrder(transactions, repository);
		orden.verify(transactions).getTransaction(any());
		orden.verify(repository).buscarPorAlias("AAAAA");
		orden.verify(repository).insertar(any(Enlace.class));
		orden.verify(transactions).rollback(primero);
		orden.verify(transactions).getTransaction(any());
		orden.verify(repository).buscarPorAlias("BBBBB");
		orden.verify(repository).insertar(enlace);
		orden.verify(transactions).commit(segundo);
		verify(generadorAlias, times(2)).generar(any());
	}

	@Test
	void propagaOtrosErroresSinReintentar() {
		RuntimeException error = new IllegalStateException("Persistencia no disponible");
		doThrow(error).when(repository).insertar(any(Enlace.class));

		assertThatThrownBy(() -> service.crear("https://example.org")).isSameAs(error);
		verify(generadorAlias).generar(any());
		verify(transactions).rollback(any());
		verify(transactions, never()).commit(any());
	}

	@Test
	void fallaSiSeAgotanLosIntentosPorColisiones() {
		doThrow(new ColisionAliasException(new IllegalStateException("Alias ocupado")))
				.when(repository).insertar(any(Enlace.class));

		assertThatThrownBy(() -> service.crear("https://example.org"))
				.isInstanceOf(AliasNoDisponibleException.class);
		verify(generadorAlias, times(3)).generar(any());
		verify(transactions, times(3)).rollback(any());
		verify(transactions, never()).commit(any());
	}

	@Test
	void pruebaOtroAliasSiLaReasignacionNoFuePosible() {
		Enlace existente = new Enlace("AAAAA", "https://example.org/anterior",
				AHORA.minusSeconds(3600), AHORA);
		when(repository.buscarPorAlias("AAAAA")).thenReturn(Optional.of(existente));
		when(repository.reasignarSiEstaVencido(any(), any(), any(), any())).thenReturn(false);

		Enlace creado = service.crear("https://example.org/nuevo");

		assertThat(creado.getAlias()).isEqualTo("BBBBB");
		verify(repository).reasignarSiEstaVencido(existente, "https://example.org/nuevo",
				AHORA, AHORA.plusSeconds(3600));
		verify(repository).insertar(creado);
		verify(generadorAlias, times(2)).generar(any());
	}

	@Test
	void devuelveElEnlaceReasignadoSinInsertarOtraFila() {
		Enlace existente = new Enlace("AAAAA", "https://example.org/anterior",
				AHORA.minusSeconds(3600), AHORA);
		when(repository.buscarPorAlias("AAAAA")).thenReturn(Optional.of(existente));
		when(repository.reasignarSiEstaVencido(eq(existente), any(), any(), any())).thenReturn(true);

		assertThat(service.crear("https://example.org/nuevo")).isSameAs(existente);
		verify(repository, never()).insertar(any());
		verify(transactions).commit(any());
	}

	@Test
	void lePasaAlGeneradorLosAliasVigentesDentroDeLaTransaccion() {
		when(repository.buscarAliasVigentes(AHORA)).thenReturn(Set.of("1", "2"));
		when(generadorAlias.generar(Set.of("1", "2"))).thenReturn("3");

		assertThat(service.crear("https://example.org").getAlias()).isEqualTo("3");
		InOrder orden = inOrder(transactions, repository, generadorAlias);
		orden.verify(transactions).getTransaction(any());
		orden.verify(repository).buscarAliasVigentes(AHORA);
		orden.verify(generadorAlias).generar(Set.of("1", "2"));
		orden.verify(repository).insertar(any(Enlace.class));
		orden.verify(transactions).commit(any());
	}

	@Test
	void rechazaLaUrlAntesDeGenerarAliasOAccederALaBase() {
		assertThatThrownBy(() -> service.crear("ftp://example.org"))
				.isInstanceOf(UrlInvalidaException.class);
		verify(generadorAlias, never()).generar(any());
		verify(repository, never()).buscarPorAlias(any());
		verify(transactions, never()).getTransaction(any());
	}
}
