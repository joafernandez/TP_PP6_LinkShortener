package app.qr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/** Genera el PNG de un QR en memoria, sin acceder a enlaces ni conocer HTTP (ADR-0023). */
@Service
public class CodigoQrService {

	private static final int TAMANO_IMAGEN = 300;

	/** Codifica exactamente el texto recibido en un QR de 300 × 300 píxeles. */
	public byte[] generarPng(String texto) throws WriterException, IOException {
		Map<EncodeHintType, Object> opciones = Map.of(
				EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
		BitMatrix matriz = new QRCodeWriter().encode(
				texto, BarcodeFormat.QR_CODE, TAMANO_IMAGEN, TAMANO_IMAGEN, opciones);

		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
		return salida.toByteArray();
	}
}
