package app.enlace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifica el Repository real, incluida la traducción de errores de Spring y los UPDATE condicionales. */
@SpringBootTest
class EnlaceRepositoryIT {

	private static final Instant AHORA = Instant.parse("2026-10-11T18:00:00Z");

	@Autowired
	private EnlaceRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbc;

	private TransactionTemplate transaction;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		jdbc.update("DELETE FROM link");
	}

	@Test
	void traduceUnaColisionRealYPermiteContinuarEnOtraTransaccion() {
		transaction.executeWithoutResult(status -> repository.insertar(
				new Enlace("AAAAA", "https://example.org/primero", AHORA, AHORA.plusSeconds(3600))));

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> repository.insertar(
				new Enlace("AAAAA", "https://example.org/duplicado", AHORA, AHORA.plusSeconds(3600)))))
				.isInstanceOf(ColisionAliasException.class)
				.hasCauseInstanceOf(ConstraintViolationException.class);

		transaction.executeWithoutResult(status -> repository.insertar(
				new Enlace("BBBBB", "https://example.org/segundo", AHORA, AHORA.plusSeconds(3600))));
		transaction.executeWithoutResult(status -> {
			assertThat(repository.buscarPorAlias("AAAAA").orElseThrow().getUrlOriginal())
					.isEqualTo("https://example.org/primero");
			assertThat(repository.buscarPorAlias("BBBBB")).isPresent();
		});
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link", Integer.class)).isEqualTo(2);
	}

	@Test
	void buscaSoloLosAliasVigentes() {
		transaction.executeWithoutResult(status -> {
			repository.insertar(new Enlace("1", "https://example.org/vigente", AHORA, AHORA.plusSeconds(3600)));
			repository.insertar(new Enlace("2", "https://example.org/vencido", AHORA.minusSeconds(3600), AHORA));
			repository.insertar(new Enlace("3", "https://example.org/por-vencer", AHORA, AHORA.plusNanos(1000)));
		});

		// vence exactamente en AHORA: ya está libre, igual que en Enlace.estaVencido
		Set<String> vigentes = transaction.execute(status -> repository.buscarAliasVigentes(AHORA));

		assertThat(vigentes).containsExactlyInAnyOrder("1", "3");
	}

	@Test
	void unaLecturaAnteriorNoPermitePisarUnAliasQueYaFueRenovado() {
		transaction.executeWithoutResult(status -> repository.insertar(
				new Enlace("AAAAA", "https://example.org/vencido", AHORA.minusSeconds(3600), AHORA)));
		Enlace lecturaAnterior = transaction.execute(status -> repository.buscarPorAlias("AAAAA").orElseThrow());

		transaction.executeWithoutResult(status -> {
			Enlace actual = repository.buscarPorAlias("AAAAA").orElseThrow();
			assertThat(repository.reasignarSiEstaVencido(actual, "https://example.org/renovado",
					AHORA, AHORA.plusSeconds(3600))).isTrue();
			assertThat(actual.getUrlOriginal()).isEqualTo("https://example.org/renovado");
			assertThat(actual.getCreadoEn()).isEqualTo(AHORA);
			assertThat(actual.getVenceEn()).isEqualTo(AHORA.plusSeconds(3600));
		});

		Boolean reasignado = transaction.execute(status -> repository.reasignarSiEstaVencido(
				lecturaAnterior, "https://example.org/no-debe-guardarse", AHORA, AHORA.plusSeconds(3600)));
		assertThat(reasignado).isFalse();
		Enlace persistido = transaction.execute(status -> repository.buscarPorAlias("AAAAA").orElseThrow());
		assertThat(persistido.getId()).isEqualTo(lecturaAnterior.getId());
		assertThat(persistido.getUrlOriginal()).isEqualTo("https://example.org/renovado");
		assertThat(persistido.getVenceEn()).isEqualTo(AHORA.plusSeconds(3600));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM link", Integer.class)).isEqualTo(1);
	}
}
