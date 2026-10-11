package app.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj de prueba cuya hora se fija y se avanza manualmente. */
public class MutableClock extends Clock {

	private Instant ahora;

	public MutableClock(Instant ahora) {
		this.ahora = ahora;
	}

	public void set(Instant ahora) {
		this.ahora = ahora;
	}

	public void advance(Duration duration) {
		this.ahora = ahora.plus(duration);
	}

	@Override
	public Instant instant() {
		return ahora;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}
}
