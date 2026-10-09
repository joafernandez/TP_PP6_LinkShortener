package app.link;

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
import app.common.config.AppProperties.LinkSettings;
import app.link.alias.AliasGenerator;

/** Verifica los reintentos por colisión y la propagación de otros errores de persistencia. */
class LinkServiceTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
	private final AliasGenerator aliases = mock(AliasGenerator.class);
	private LinkService service;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		TypedQuery<Link> query = mock(TypedQuery.class);
		when(entityManager.createQuery(anyString(), eq(Link.class))).thenReturn(query);
		when(query.setParameter(eq("alias"), any())).thenReturn(query);
		when(query.getResultStream()).thenAnswer(invocation -> Stream.empty());
		when(transactions.getTransaction(any())).thenAnswer(invocation -> mock(TransactionStatus.class));
		when(aliases.generate()).thenReturn("AAAAA", "BBBBB", "CCCCC");
		AppProperties properties = new AppProperties("http://short.test",
				new LinkSettings(Duration.ofMinutes(60)),
				new AliasSettings(5, "ABC", 3, Set.of()));
		service = new LinkService(entityManager, transactions, aliases, new UrlValidator(properties),
				properties, Clock.fixed(Instant.parse("2026-10-05T13:00:00Z"), ZoneOffset.UTC));
	}

	@ParameterizedTest
	@ValueSource(strings = { "uk_link_alias", "UK_LINK_ALIAS", "PUBLIC.UK_LINK_ALIAS" })
	void reintentaLaColisionDelAliasEnOtraTransaccion(String constraint) {
		RuntimeException collision = new DataIntegrityViolationException("Inserción rechazada",
				violation(constraint, "23505"));
		doThrow(collision).doNothing().when(entityManager).flush();

		Link link = service.create("https://example.org");

		assertThat(link.getAlias()).isEqualTo("BBBBB");
		verify(aliases, times(2)).generate();
		verify(transactions, times(2)).getTransaction(any());
		verify(transactions).rollback(any());
		verify(transactions).commit(any());
	}

	@Test
	void reconoceLaColisionDentroDeExcepcionesAnidadas() {
		RuntimeException collision = new PersistenceException("Persistencia",
				new RuntimeException("Causa intermedia", violation("UK_LINK_ALIAS", "23505")));
		doThrow(collision).doNothing().when(entityManager).flush();

		assertThat(service.create("https://example.org").getAlias()).isEqualTo("BBBBB");
		verify(aliases, times(2)).generate();
	}

	@ParameterizedTest
	@MethodSource("otherPersistenceErrors")
	void propagaElErrorOriginalSinReintentar(RuntimeException error) {
		doThrow(error).when(entityManager).flush();

		assertThatThrownBy(() -> service.create("https://example.org")).isSameAs(error);
		verify(aliases).generate();
		verify(entityManager).flush();
		verify(transactions).rollback(any());
		verify(transactions, never()).commit(any());
	}

	@Test
	void fallaSiSeAgotanLosReintentosPorColisionesConfirmadas() {
		doThrow(violation("UK_LINK_ALIAS", "23505")).when(entityManager).flush();

		assertThatThrownBy(() -> service.create("https://example.org"))
				.isInstanceOf(AliasUnavailableException.class);
		verify(aliases, times(3)).generate();
		verify(transactions, times(3)).rollback(any());
		verify(transactions, never()).commit(any());
	}

	private static Stream<RuntimeException> otherPersistenceErrors() {
		return Stream.of(
				new PersistenceException("Error de persistencia sin colisión"),
				new DataIntegrityViolationException("Otra restricción", violation("uk_otro_campo", "23505")),
				violation(null, "23505"),
				violation("UK_LINK_ALIAS_EXTRA", "23505"),
				violation("UK_LINK_ALIAS", "23502"),
				new DataIntegrityViolationException("UK_LINK_ALIAS", new SQLException("UK_LINK_ALIAS", "23505")));
	}

	private static ConstraintViolationException violation(String constraint, String sqlState) {
		return new ConstraintViolationException("Restricción violada",
				new SQLException("Inserción rechazada", sqlState), constraint);
	}
}
