package app.qrlogin.dto;

import app.qrlogin.EstadoQrLogin;
import io.swagger.v3.oas.annotations.media.Schema;

/** Estado de un pedido de login con QR. */
@Schema(name = "QrLoginStatusResponse")
public record QrLoginStatusResponse(EstadoQrLogin status) {
}
