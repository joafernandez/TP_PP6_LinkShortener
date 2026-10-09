package app.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Comprueba la validación al crear la configuración de Spring, sin levantar la base de datos. */
class AppPropertiesTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfig.class)
			.withPropertyValues(
					"app.base-url=http://localhost:8080",
					"app.link.ttl=60m",
					"app.alias.length=5",
					"app.alias.alphabet=23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz",
					"app.alias.max-attempts=10",
					"app.alias.reserved=api,error,v3,swagger");

	@Test
	void arrancaConLosValoresActuales() {
		runner.run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(AppProperties.class);
			AppProperties properties = context.getBean(AppProperties.class);
			assertThat(properties.baseUrl()).isEqualTo("http://localhost:8080");
			assertThat(properties.link().ttl()).isEqualTo(Duration.ofMinutes(60));
			assertThat(properties.alias().length()).isEqualTo(5);
		});
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 16 })
	void aceptaLosLimitesDelLargoDelAlias(int length) {
		runner.withPropertyValues("app.alias.length=" + length).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(AppProperties.class).alias().length()).isEqualTo(length);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://localhost:8081/", "https://example.org", "http://192.168.1.50", "HTTPS://EXAMPLE.ORG" })
	void aceptaUrlsBaseHttpYHttpsConHost(String baseUrl) {
		runner.withPropertyValues("app.base-url=" + baseUrl).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(AppProperties.class).baseUrl()).isEqualTo(baseUrl);
		});
	}

	@ParameterizedTest
	@CsvSource({
			"app.alias.length=0, app.alias.length debe ser al menos 1.",
			"app.alias.length=17, app.alias.length no puede superar 16.",
			"app.link.ttl=0s, app.link.ttl debe ser una duración positiva.",
			"app.link.ttl=-1s, app.link.ttl debe ser una duración positiva.",
			"app.base-url=ftp://example.org, app.base-url debe ser una URL HTTP o HTTPS con un host válido.",
			"app.base-url=example.org, app.base-url debe ser una URL HTTP o HTTPS con un host válido.",
			"app.base-url=http:///ruta, app.base-url debe ser una URL HTTP o HTTPS con un host válido.",
			"app.base-url=https://exa mple.org, app.base-url debe ser una URL HTTP o HTTPS con un host válido.",
			"app.base-url=, app.base-url es obligatoria." })
	void impideElArranqueEInformaLaConfiguracionInvalida(String property, String message) {
		runner.withPropertyValues(property).run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseInstanceOf(BindValidationException.class)
					.hasStackTraceContaining(message);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AppProperties.class)
	static class PropertiesConfig {
	}
}
