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
public class QrCodeService {

	private static final int IMAGE_SIZE = 300;

	/** Codifica exactamente el texto recibido en un QR de 300 × 300 píxeles. */
	public byte[] generatePng(String text) throws WriterException, IOException {
		Map<EncodeHintType, Object> hints = Map.of(
				EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
		BitMatrix matrix = new QRCodeWriter().encode(
				text, BarcodeFormat.QR_CODE, IMAGE_SIZE, IMAGE_SIZE, hints);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(matrix, "PNG", output);
		return output.toByteArray();
	}
}
