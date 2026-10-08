package app.qr;

import java.io.IOException;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.zxing.WriterException;

import app.link.Link;
import app.link.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** QR de un enlace vigente (contrato: docs/api/openapi.yaml, ADR-0023). */
@RestController
@RequestMapping("/api/v1/links")
@Tag(name = "QR", description = "Código QR de un enlace")
public class QrController {

	private final LinkService linkService;
	private final QrCodeService qrCodeService;

	public QrController(LinkService linkService, QrCodeService qrCodeService) {
		this.linkService = linkService;
		this.qrCodeService = qrCodeService;
	}

	@Operation(operationId = "getLinkQr", summary = "Obtener el código QR de un enlace vigente",
			description = "PNG de 300 × 300 píxeles de la URL corta. No crea enlaces ni renueva su vigencia.")
	@ApiResponse(responseCode = "200", description = "Imagen PNG del código QR",
			content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE,
					schema = @Schema(type = "string", contentMediaType = MediaType.IMAGE_PNG_VALUE)),
			headers = @Header(name = "Cache-Control", description = "Evita almacenar la respuesta.",
					schema = @Schema(type = "string", allowableValues = "no-store")))
	@ApiResponse(responseCode = "404", description = "El alias no existe o el enlace está vencido.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)),
			headers = @Header(name = "Cache-Control", description = "Evita almacenar el error.",
					schema = @Schema(type = "string", allowableValues = "no-store")))
	@GetMapping(value = "/{alias}/qr", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getQr(
			@Parameter(description = "Alias del enlace", example = "xT3se",
					schema = @Schema(pattern = "^[A-Za-z0-9]+$"))
			@PathVariable String alias) throws WriterException, IOException {
		Link link = linkService.resolve(alias);
		byte[] png = qrCodeService.generatePng(linkService.shortUrlOf(link));
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.cacheControl(CacheControl.noStore())
				.body(png);
	}
}
