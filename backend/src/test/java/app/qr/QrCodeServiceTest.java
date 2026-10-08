package app.qr;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

/** Comprueba el formato, las dimensiones y el contenido real del QR, sin iniciar Spring. */
class QrCodeServiceTest {

	private final QrCodeService service = new QrCodeService();

	@Test
	void generaUnPngDe300Por300Pixeles() throws Exception {
		byte[] png = service.generatePng("http://short.test/xT3se");

		assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47,
				(byte) 0x0d, (byte) 0x0a, (byte) 0x1a, (byte) 0x0a);
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
		assertThat(image).isNotNull();
		assertThat(image.getWidth()).isEqualTo(300);
		assertThat(image.getHeight()).isEqualTo(300);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://short.test/xT3se",
			"https://example.org/café?saludo=¡Hola!" })
	void elQrCodificaExactamenteElTextoRecibido(String text) throws Exception {
		byte[] png = service.generatePng(text);
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
		BinaryBitmap bitmap = new BinaryBitmap(
				new HybridBinarizer(new BufferedImageLuminanceSource(image)));

		// Es un PNG de un único QR, sin fondo ni perspectiva de una fotografía.
		String decodedText = new MultiFormatReader()
				.decode(bitmap, Map.of(DecodeHintType.PURE_BARCODE, true)).getText();

		assertThat(decodedText).isEqualTo(text);
	}
}
