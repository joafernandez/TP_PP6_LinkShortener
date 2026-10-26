package app.espacio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import app.usuario.Usuario;
import app.usuario.UsuarioRepository;

/** La unicidad del espacio la garantiza la base de datos (ADR-0028), no solo el código. */
@SpringBootTest
class EspacioRepositoryIT {

	private static final Instant AHORA = Instant.parse("2026-10-25T18:00:00Z");

	@Autowired
	private EspacioRepository repository;

	@Autowired
	private UsuarioRepository usuarioRepository;

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
	void guardaElEspacioEnMinusculasYLoEncuentraSinDistinguirMayusculas() {
		Usuario melina = crearUsuario("google-melina");
		transaction.executeWithoutResult(status -> repository.insertar(new Espacio("Melina", melina)));

		assertThat(jdbc.queryForObject("SELECT name FROM namespace", String.class)).isEqualTo("melina");
		transaction.executeWithoutResult(status -> {
			assertThat(repository.buscarPorNombre("MELINA")).isPresent();
			assertThat(repository.buscarPorNombre("melina").orElseThrow().getUsuario().getId())
					.isEqualTo(melina.getId());
			assertThat(repository.buscarPorUsuario(melina.getId()).orElseThrow().getNombre()).isEqualTo("melina");
		});
	}

	@Test
	void noEncuentraEspaciosInexistentes() {
		Usuario sinEspacio = crearUsuario("google-sin-espacio");

		transaction.executeWithoutResult(status -> {
			assertThat(repository.buscarPorNombre("nadie")).isEmpty();
			assertThat(repository.buscarPorUsuario(sinEspacio.getId())).isEmpty();
		});
	}

	@Test
	void dosUsuariosNoPuedenTenerElMismoEspacioAunqueCambienLasMayusculas() {
		Usuario melina = crearUsuario("google-melina");
		Usuario otra = crearUsuario("google-otra");
		transaction.executeWithoutResult(status -> repository.insertar(new Espacio("melina", melina)));

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> repository.insertar(
				new Espacio("MELINA", otra))))
				.isInstanceOf(EspacioOcupadoException.class)
				.hasMessageContaining("melina");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace", Integer.class)).isEqualTo(1);
	}

	@Test
	void unUsuarioNoPuedeTenerDosEspacios() {
		Usuario melina = crearUsuario("google-melina");
		transaction.executeWithoutResult(status -> repository.insertar(new Espacio("melina", melina)));

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> repository.insertar(
				new Espacio("melina2", melina))))
				.isInstanceOf(DataIntegrityViolationException.class)
				.isNotInstanceOf(EspacioOcupadoException.class);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace", Integer.class)).isEqualTo(1);
	}

	@Test
	void laBaseRechazaCaracteresNoPermitidosAunqueSeEscribaDirecto() {
		Usuario melina = crearUsuario("google-melina");

		for (String invalido : new String[] { "Melina", "mel ina", "mel-ina", "mel_ina", "melina!" }) {
			assertThatThrownBy(() -> jdbc.update("INSERT INTO namespace (name, user_id) VALUES (?, ?)",
					invalido, melina.getId()))
					.as(invalido)
					.isInstanceOf(DataIntegrityViolationException.class);
		}
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM namespace", Integer.class)).isZero();
	}

	@Test
	void laBaseAceptaHasta15CaracteresYRechazaMas() {
		Usuario melina = crearUsuario("google-melina");
		Usuario otra = crearUsuario("google-otra");

		jdbc.update("INSERT INTO namespace (name, user_id) VALUES (?, ?)", "a".repeat(15), melina.getId());
		assertThatThrownBy(() -> jdbc.update("INSERT INTO namespace (name, user_id) VALUES (?, ?)",
				"b".repeat(16), otra.getId()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Usuario crearUsuario(String googleSub) {
		Usuario usuario = new Usuario(googleSub, googleSub + "@example.org", googleSub, AHORA);
		transaction.executeWithoutResult(status -> usuarioRepository.insertar(usuario));
		return usuario;
	}
}
