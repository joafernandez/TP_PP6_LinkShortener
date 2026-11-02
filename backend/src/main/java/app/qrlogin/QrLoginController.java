package app.qrlogin;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.zxing.WriterException;

import app.qr.CodigoQrService;
import app.qrlogin.dto.QrLoginResponse;
import app.qrlogin.dto.QrLoginStatusResponse;
import app.usuario.SesionRequeridaException;
import app.usuario.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Login con QR desde el celular (contrato: docs/api/openapi.yaml, ADR-0033). */
@RestController
@RequestMapping("/api/v1/auth/qr-logins")
@Tag(name = "Login con QR", description = "Iniciar sesión en la computadora autorizándolo desde el celular")
public class QrLoginController {

	static final String ENCABEZADO_SECRETO = "X-Qr-Secret";
	private static final int LARGO_MAXIMO_DEL_CODIGO = 64;

	private final QrLoginService qrLoginService;
	private final CodigoQrService codigoQrService;
	private final IniciadorDeSesion iniciadorDeSesion;

	public QrLoginController(QrLoginService qrLoginService, CodigoQrService codigoQrService,
			IniciadorDeSesion iniciadorDeSesion) {
		this.qrLoginService = qrLoginService;
		this.codigoQrService = codigoQrService;
		this.iniciadorDeSesion = iniciadorDeSesion;
	}

	@PostMapping
	@Operation(operationId = "createQrLogin", summary = "Crear un pedido de login con QR",
			description = "La computadora lo pide sin sesión. Devuelve el código del QR y un secreto que solo ella conoce.")
	public ResponseEntity<QrLoginResponse> crear() {
		QrLogin qrLogin = qrLoginService.crear();
		return ResponseEntity.status(HttpStatus.CREATED)
				.cacheControl(CacheControl.noStore())
				.body(QrLoginResponse.desde(qrLogin));
	}

	@GetMapping(value = "/{codigo:[A-Za-z0-9_-]+}/qr", produces = MediaType.IMAGE_PNG_VALUE)
	@Operation(operationId = "getQrLoginQr", summary = "Imagen del QR de un pedido de login",
			description = "PNG que codifica la dirección de la página de autorización del celular.")
	public ResponseEntity<byte[]> qr(@PathVariable String codigo) throws WriterException, IOException {
		if (codigo.length() > LARGO_MAXIMO_DEL_CODIGO) {
			throw new QrLoginNoEncontradoException();
		}
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.cacheControl(CacheControl.noStore())
				.body(codigoQrService.generarPng(qrLoginService.urlParaElCelular(codigo)));
	}

	@GetMapping("/{codigo:[A-Za-z0-9_-]+}")
	@Operation(operationId = "getQrLoginStatus", summary = "Consultar el estado de un pedido de login",
			description = "La computadora lo consulta cada 2 segundos con su secreto en el encabezado X-Qr-Secret.")
	public ResponseEntity<QrLoginStatusResponse> estado(@PathVariable String codigo,
			@RequestHeader(ENCABEZADO_SECRETO) String secreto) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(new QrLoginStatusResponse(qrLoginService.estado(codigo, secreto)));
	}

	@PostMapping("/{codigo:[A-Za-z0-9_-]+}/session")
	@Operation(operationId = "createQrLoginSession", summary = "Canjear un pedido autorizado por una sesión",
			description = "Abre la sesión en la computadora que hizo el pedido. Funciona una sola vez.")
	public ResponseEntity<Void> iniciarSesion(@PathVariable String codigo,
			@RequestHeader(ENCABEZADO_SECRETO) String secreto,
			HttpServletRequest request, HttpServletResponse response) {
		Usuario usuario = qrLoginService.iniciarSesion(codigo, secreto);
		iniciadorDeSesion.iniciar(usuario, request, response);
		return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
	}

	@PostMapping("/{codigo:[A-Za-z0-9_-]+}/authorize")
	@Operation(operationId = "authorizeQrLogin", summary = "Autorizar el ingreso en la computadora",
			description = "Lo hace el celular con sesión de Google. Requiere el encabezado X-XSRF-TOKEN.")
	public ResponseEntity<Void> autorizar(@PathVariable String codigo, @AuthenticationPrincipal OidcUser usuarioGoogle) {
		qrLoginService.autorizar(codigo, subDe(usuarioGoogle));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{codigo:[A-Za-z0-9_-]+}/reject")
	@Operation(operationId = "rejectQrLogin", summary = "Rechazar el ingreso en la computadora",
			description = "Lo hace el celular con sesión de Google. Requiere el encabezado X-XSRF-TOKEN.")
	public ResponseEntity<Void> rechazar(@PathVariable String codigo, @AuthenticationPrincipal OidcUser usuarioGoogle) {
		qrLoginService.rechazar(codigo, subDe(usuarioGoogle));
		return ResponseEntity.noContent().build();
	}

	private static String subDe(OidcUser usuarioGoogle) {
		if (usuarioGoogle == null) {
			throw new SesionRequeridaException();
		}
		return usuarioGoogle.getSubject();
	}
}
