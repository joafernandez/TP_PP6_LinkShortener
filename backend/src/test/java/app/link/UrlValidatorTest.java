package app.link;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;
import app.common.config.AppProperties.LinkSettings;

class UrlValidatorTest {

	private final UrlValidator validator = validatorWithBaseUrl("http://192.168.1.50");

	@ParameterizedTest
	@ValueSource(strings = {
			"http://example.org",
			"https://example.org",
			"HTTPS://EXAMPLE.ORG/Ruta",
			"https://drive.google.com/drive/folders/1AbCdEf?usp=sharing",
			"http://192.168.1.50:8080/otro-servicio",
			"https://192.168.1.50/xT3se",
			"http://localhost:3000/a?b=c#d" })
	void aceptaUrlsValidas(String url) {
		assertThatCode(() -> validator.validate(url)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void rechazaUrlVacia(String url) {
		assertInvalid(url);
	}

	@Test
	void aceptaHasta2048Caracteres() {
		String url = urlOfLength(2048);

		assertThatCode(() -> validator.validate(url)).doesNotThrowAnyException();
	}

	@Test
	void rechazaMasDe2048Caracteres() {
		assertInvalid(urlOfLength(2049));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"ftp://example.org/archivo",
			"mailto:alguien@example.org",
			"javascript:alert(1)",
			"file:///C:/secreto.txt",
			"data:text/html,hola" })
	void rechazaEsquemasDistintosDeHttpYHttps(String url) {
		assertInvalid(url);
	}

	@ParameterizedTest
	@ValueSource(strings = { "example.org", "/ruta/relativa", "www.example.org/pagina" })
	void rechazaUrlsRelativas(String url) {
		assertInvalid(url);
	}

	@ParameterizedTest
	@ValueSource(strings = { "http://", "http:///ruta", "https:ruta" })
	void rechazaUrlsSinHost(String url) {
		assertInvalid(url);
	}

	@ParameterizedTest
	@ValueSource(strings = { "http://exa mple.org", "http://example.org/<script>" })
	void rechazaUrlsMalFormadas(String url) {
		assertInvalid(url);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://192.168.1.50/xT3se",
			"http://192.168.1.50:80/xT3se",
			"HTTP://192.168.1.50/abcde" })
	void rechazaUrlsDelPropioServicio(String url) {
		assertInvalid(url);
	}

	@Test
	void comparaHostSinDistinguirMayusculas() {
		UrlValidator conDominio = validatorWithBaseUrl("https://corto.example.org");

		assertThatThrownBy(() -> conDominio.validate("https://CORTO.example.org:443/abcde"))
				.isInstanceOf(InvalidUrlException.class);
	}

	private void assertInvalid(String url) {
		assertThatThrownBy(() -> validator.validate(url)).isInstanceOf(InvalidUrlException.class);
	}

	private static String urlOfLength(int length) {
		String prefix = "https://example.org/";
		return prefix + "a".repeat(length - prefix.length());
	}

	private static UrlValidator validatorWithBaseUrl(String baseUrl) {
		return new UrlValidator(new AppProperties(baseUrl,
				new LinkSettings(Duration.ofMinutes(60)),
				new AliasSettings(5, "abc", 10, Set.of())));
	}
}
