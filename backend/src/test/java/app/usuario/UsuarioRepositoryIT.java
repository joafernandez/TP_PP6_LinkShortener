package app.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifica el Repository real de usuarios sobre HSQLDB en memoria (ADR-0028). */
@SpringBootTest
class UsuarioRepositoryIT {

	private static final Instant AHORA = Instant.parse("2026-10-25T18:00:00Z");

	@Autowired
	private UsuarioRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbc;

	private TransactionTemplate transaction;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		jdbc.update("DELETE FROM namespace");
		jdbc.update("DELETE FROM app_user");
	}

	@Test
	void guardaYBuscaUnUsuarioPorSuIdentificadorDeGoogle() {
		transaction.executeWithoutResult(status -> repository.insertar(
				new Usuario("google-123", "melina@example.org", "Melina", AHORA)));

		Usuario encontrado = transaction.execute(status -> repository.buscarPorGoogleSub("google-123").orElseThrow());

		assertThat(encontrado.getId()).isNotNull();
		assertThat(encontrado.getEmail()).isEqualTo("melina@example.org");
		assertThat(encontrado.getNombre()).isEqualTo("Melina");
		assertThat(encontrado.getCreadoEn()).isEqualTo(AHORA);
	}

	@Test
	void noEncuentraUnUsuarioInexistente() {
		Optional<Usuario> encontrado = transaction.execute(status -> repository.buscarPorGoogleSub("no-existe"));

		assertThat(encontrado).isEmpty();
	}

	@Test
	void laBaseRechazaElMismoUsuarioDeGoogleDosVeces() {
		transaction.executeWithoutResult(status -> repository.insertar(
				new Usuario("google-123", "melina@example.org", "Melina", AHORA)));

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> repository.insertar(
				new Usuario("google-123", "otro@example.org", "Otro", AHORA))))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class)).isEqualTo(1);
	}
}
