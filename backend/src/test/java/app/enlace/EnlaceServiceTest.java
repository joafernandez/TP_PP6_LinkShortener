package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;
import app.common.config.AppProperties.EnlaceSettings;
import app.enlace.alias.GeneradorAlias;

/** Verifica los reintentos por colisión y la propagación de otros errores de persistencia. */
class EnlaceServiceTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
	private final GeneradorAlias aliases = mock(GeneradorAlias.class);
	private EnlaceService service;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		TypedQuery<Enlace> query = mock(TypedQuery.class);
		when(entityManager.createQuery(anyString(), eq(Enlace.class))).thenReturn(query);
		when(query.setParameter(eq("alias"), any())).thenReturn(query);
		when(query.getResultStream()).thenAnswer(invocation -> Stream.empty());
		when(transactions.getTransaction(any())).thenAnswer(invocation -> mock(TransactionStatus.class));
		when(aliases.generar()).thenReturn("AAAAA", "BBBBB", "CCCCC");
		AppProperties propiedades = new AppProperties("http://short.test",
				new EnlaceSettings(Duration.ofMinutes(60)),
				new AliasSettings(5, "ABC", 3, Set.of()));
		service = new EnlaceService(entityManager, transactions, aliases, new ValidadorUrl(propiedades),
				propiedades, Clock.fixed(Instant.parse("2026-10-05T13:00:00Z"), ZoneOffset.UTC));
	}

	@ParameterizedTest
	@ValueSource(strings = { "uk_link_alias", "UK_LINK_ALIAS", "PUBLIC.UK_LINK_ALIAS" })
	void reintentaLaColisionDelAliasEnOtraTransaccion(String restriccion) {
		RuntimeException collision = new DataIntegrityViolationException("Inserción rechazada",
				violacion(restriccion, "23505"));
		doThrow(collision).doNothing().when(entityManager).flush();

		Enlace enlace = service.crear("https://example.org");

		assertThat(enlace.getAlias()).isEqualTo("BBBBB");
		verify(aliases, times(2)).generar();
		verify(transactions, times(2)).getTransaction(any());
		verify(transactions).rollback(any());
		verify(transactions).commit(any());
	}

	@Test
	void reconoceLaColisionDentroDeExcepcionesAnidadas() {
		RuntimeException collision = new PersistenceException("Persistencia",
				new RuntimeException("Causa intermedia", violacion("UK_LINK_ALIAS", "23505")));
		doThrow(collision).doNothing().when(entityManager).flush();

		assertThat(service.crear("https://example.org").getAlias()).isEqualTo("BBBBB");
		verify(aliases, times(2)).generar();
	}

	@ParameterizedTest
	@MethodSource("otherPersistenceErrors")
	void propagaElErrorOriginalSinReintentar(RuntimeException error) {
		doThrow(error).when(entityManager).flush();

		assertThatThrownBy(() -> service.crear("https://example.org")).isSameAs(error);
		verify(aliases).generar();
		verify(entityManager).flush();
		verify(transactions).rollback(any());
		verify(transactions, never()).commit(any());
	}

	@Test
	void fallaSiSeAgotanLosReintentosPorColisionesConfirmadas() {
		doThrow(violacion("UK_LINK_ALIAS", "23505")).when(entityManager).flush();

		assertThatThrownBy(() -> service.crear("https://example.org"))
				.isInstanceOf(AliasNoDisponibleException.class);
		verify(aliases, times(3)).generar();
		verify(transactions, times(3)).rollback(any());
		verify(transactions, never()).commit(any());
	}

	private static Stream<RuntimeException> otherPersistenceErrors() {
		return Stream.of(
				new PersistenceException("Error de persistencia sin colisión"),
				new DataIntegrityViolationException("Otra restricción", violacion("uk_otro_campo", "23505")),
				violacion(null, "23505"),
				violacion("UK_LINK_ALIAS_EXTRA", "23505"),
				violacion("UK_LINK_ALIAS", "23502"),
				new DataIntegrityViolationException("UK_LINK_ALIAS", new SQLException("UK_LINK_ALIAS", "23505")));
	}

	private static ConstraintViolationException violacion(String restriccion, String sqlState) {
		return new ConstraintViolationException("Restricción violada",
				new SQLException("Inserción rechazada", sqlState), restriccion);
	}
}
