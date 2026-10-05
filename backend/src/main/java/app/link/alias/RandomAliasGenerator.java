package app.link.alias;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import app.common.config.AppProperties;
import app.common.config.AppProperties.AliasSettings;

/**
 * Genera alias aleatorios con el alfabeto y el largo configurados (ADR-0016).
 * Usa {@link SecureRandom} para que los alias no puedan predecirse.
 */
@Component
public class RandomAliasGenerator implements AliasGenerator {

	private final AliasSettings settings;
	private final RandomGenerator random;

	@Autowired
	public RandomAliasGenerator(AppProperties properties) {
		this(properties.alias(), new SecureRandom());
	}

	RandomAliasGenerator(AliasSettings settings, RandomGenerator random) {
		this.settings = settings;
		this.random = random;
	}

	@Override
	public String generate() {
		String alias;
		do {
			alias = randomString();
		} while (settings.reserved().contains(alias));
		return alias;
	}

	private String randomString() {
		String alphabet = settings.alphabet();
		StringBuilder sb = new StringBuilder(settings.length());
		for (int i = 0; i < settings.length(); i++) {
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return sb.toString();
	}
}
