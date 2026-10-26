package app.qrlogin.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.qrlogin.QrLogin;
import io.swagger.v3.oas.annotations.media.Schema;

/** Pedido de login con QR recién creado. El secreto solo se entrega acá, a la computadora que lo pidió. */
@Schema(name = "QrLoginResponse")
public record QrLoginResponse(
		String code,
		String secret,
		@JsonProperty("expiresAt") Instant venceEn) {

	public static QrLoginResponse desde(QrLogin qrLogin) {
		return new QrLoginResponse(qrLogin.getCodigo(), qrLogin.getSecreto(), qrLogin.getVenceEn());
	}
}
