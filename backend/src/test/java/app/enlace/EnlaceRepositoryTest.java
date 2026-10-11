package app.enlace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.dao.DataIntegrityViolationException;

/** Comprueba la clasificación técnica en persist y flush, conservando las causas originales. */
class EnlaceRepositoryTest {

	private final EntityManager entityManager = mock(EntityManager.class);
	private final EnlaceRepository repository = new EnlaceRepository(entityManager);
	private final Enlace enlace = new Enlace("AAAAA", "https://example.org", Instant.EPOCH,
			Instant.EPOCH.plusSeconds(3600));

	@ParameterizedTest
	@MethodSource("colisionesConfirmadas")
	void traduceSoloLaColisionDelAliasYConservaLaCausa(RuntimeException error, boolean durantePersist) {
		provocar(error, durantePersist);

		assertThatThrownBy(() -> repository.insertar(enlace))
				.isInstanceOf(ColisionAliasException.class)
				.hasCause(error);
		if (durantePersist) {
			verify(entityManager, never()).flush();
		}
	}

	@ParameterizedTest
	@MethodSource("otrosErrores")
	void propagaLosOtrosErroresSinClasificarlosComoColision(RuntimeException error, boolean durantePersist) {
		provocar(error, durantePersist);

		assertThatThrownBy(() -> repository.insertar(enlace)).isSameAs(error);
	}

	private void provocar(RuntimeException error, boolean durantePersist) {
		if (durantePersist) {
			doThrow(error).when(entityManager).persist(any(Enlace.class));
		} else {
			doThrow(error).when(entityManager).flush();
		}
	}

	private static Stream<Arguments> colisionesConfirmadas() {
		return Stream.of("uk_link_alias", "UK_LINK_ALIAS", "PUBLIC.UK_LINK_ALIAS")
				.flatMap(restriccion -> Stream.of(
						Arguments.of(violacion(restriccion, "23505"), true),
						Arguments.of(new DataIntegrityViolationException("Inserción rechazada",
								violacion(restriccion, "23505")), false),
						Arguments.of(new PersistenceException("Persistencia",
								new RuntimeException("Causa intermedia", violacion(restriccion, "23505"))), false)));
	}

	private static Stream<Arguments> otrosErrores() {
		return Stream.<RuntimeException>of(
				new PersistenceException("Error de persistencia sin colisión"),
				new DataIntegrityViolationException("Otra restricción", violacion("uk_otro_campo", "23505")),
				violacion(null, "23505"),
				violacion("UK_LINK_ALIAS_EXTRA", "23505"),
				violacion("UK_LINK_ALIAS", "23502"),
				new DataIntegrityViolationException("UK_LINK_ALIAS", new SQLException("UK_LINK_ALIAS", "23505")))
				.flatMap(error -> Stream.of(Arguments.of(error, true), Arguments.of(error, false)));
	}

	private static ConstraintViolationException violacion(String restriccion, String sqlState) {
		return new ConstraintViolationException("Restricción violada",
				new SQLException("Inserción rechazada", sqlState), restriccion);
	}
}
