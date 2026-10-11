package app.enlace.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.enlace.Enlace;
import io.swagger.v3.oas.annotations.media.Schema;

/** Representación de un enlace corto en la API. */
@Schema(name = "LinkResponse")
public record EnlaceResponse(
		String alias,
		@JsonProperty("shortUrl") String urlCorta,
		@JsonProperty("originalUrl") String urlOriginal,
		@JsonProperty("createdAt") Instant creadoEn,
		@JsonProperty("expiresAt") Instant venceEn) {

	public static EnlaceResponse desde(Enlace enlace, String urlCorta) {
		return new EnlaceResponse(enlace.getAlias(), urlCorta, enlace.getUrlOriginal(),
				enlace.getCreadoEn(), enlace.getVenceEn());
	}
}
