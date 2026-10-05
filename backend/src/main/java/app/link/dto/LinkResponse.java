package app.link.dto;

import java.time.Instant;

import app.link.Link;

/** Representación de un enlace corto en la API. */
public record LinkResponse(
		String alias,
		String shortUrl,
		String originalUrl,
		Instant createdAt,
		Instant expiresAt) {

	public static LinkResponse of(Link link, String shortUrl) {
		return new LinkResponse(link.getAlias(), shortUrl, link.getOriginalUrl(),
				link.getCreatedAt(), link.getExpiresAt());
	}
}
