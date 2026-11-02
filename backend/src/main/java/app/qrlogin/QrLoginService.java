package app.qrlogin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.common.config.AppProperties;
import app.usuario.SesionRequeridaException;
import app.usuario.Usuario;
import app.usuario.UsuarioRepository;

/**
 * Reglas del login con QR (ADR-0033): el pedido vence a los 2 minutos, se usa una sola vez y solo la
 * computadora que lo creó, con su secreto, puede canjearlo por una sesión.
 */
@Service
public class QrLoginService {

	/** Los pedidos vencidos se conservan un rato para que la computadora pueda informar que venció. */
	private static final Duration RETENCION_DE_VENCIDOS = Duration.ofHours(1);
	private static final int BYTES_ALEATORIOS = 32;

	private final QrLoginRepository qrLoginRepository;
	private final UsuarioRepository usuarioRepository;
	private final AppProperties propiedades;
	private final Clock clock;
	private final Duration vigencia;
	private final SecureRandom aleatorio = new SecureRandom();

	public QrLoginService(QrLoginRepository qrLoginRepository, UsuarioRepository usuarioRepository,
			AppProperties propiedades, Clock clock, @Value("${app.qr-login.ttl:2m}") Duration vigencia) {
		this.qrLoginRepository = qrLoginRepository;
		this.usuarioRepository = usuarioRepository;
		this.propiedades = propiedades;
		this.clock = clock;
		this.vigencia = vigencia;
	}

	/** Crea un pedido pendiente con un código (para el QR) y un secreto (solo para quien lo creó). */
	@Transactional
	public QrLogin crear() {
		Instant ahora = ahora();
		qrLoginRepository.eliminarVencidosAntesDe(ahora.minus(RETENCION_DE_VENCIDOS));
		QrLogin qrLogin = new QrLogin(tokenAleatorio(), tokenAleatorio(), ahora, ahora.plus(vigencia));
		qrLoginRepository.insertar(qrLogin);
		return qrLogin;
	}

	/** Dirección que abre el celular al escanear el QR: la página de autorización del servidor. */
	public String urlParaElCelular(String codigo) {
		return propiedades.urlBase().replaceAll("/+$", "") + "/qr-login.html?c=" + codigo;
	}

	/**
	 * Estado del pedido para la computadora que lo creó.
	 *
	 * @throws QrLoginNoEncontradoException si el código no existe o el secreto no corresponde
	 */
	@Transactional(readOnly = true)
	public EstadoQrLogin estado(String codigo, String secreto) {
		QrLogin qrLogin = buscarConSecreto(codigo, secreto);
		boolean sinResponder = qrLogin.getEstado() == EstadoQrLogin.PENDING
				|| qrLogin.getEstado() == EstadoQrLogin.AUTHORIZED;
		if (sinResponder && qrLogin.estaVencido(ahora())) {
			return EstadoQrLogin.EXPIRED;
		}
		return qrLogin.getEstado();
	}

	/**
	 * El usuario con sesión en el celular autoriza el ingreso en la computadora.
	 *
	 * @throws SesionRequeridaException     si el usuario de la sesión no está registrado
	 * @throws QrLoginNoEncontradoException si el código no existe
	 * @throws QrLoginNoVigenteException    si el pedido venció o ya fue respondido
	 */
	@Transactional
	public void autorizar(String codigo, String googleSub) {
		responder(codigo, googleSub, EstadoQrLogin.AUTHORIZED);
	}

	/** Igual que {@link #autorizar}, pero rechaza el ingreso. */
	@Transactional
	public void rechazar(String codigo, String googleSub) {
		responder(codigo, googleSub, EstadoQrLogin.REJECTED);
	}

	/**
	 * La computadora canjea el pedido autorizado por el usuario que lo autorizó. Solo funciona una vez.
	 *
	 * @throws QrLoginNoEncontradoException si el código no existe o el secreto no corresponde
	 * @throws QrLoginNoVigenteException    si no fue autorizado, fue rechazado, venció o ya se usó
	 */
	@Transactional
	public Usuario iniciarSesion(String codigo, String secreto) {
		buscarConSecreto(codigo, secreto);
		if (qrLoginRepository.consumirSiAutorizado(codigo, ahora()) == 0) {
			throw new QrLoginNoVigenteException();
		}
		Long usuarioId = qrLoginRepository.buscarUsuarioId(codigo).orElseThrow(QrLoginNoVigenteException::new);
		return usuarioRepository.buscarPorId(usuarioId).orElseThrow(QrLoginNoVigenteException::new);
	}

	private void responder(String codigo, String googleSub, EstadoQrLogin nuevoEstado) {
		Usuario usuario = usuarioRepository.buscarPorGoogleSub(googleSub).orElseThrow(SesionRequeridaException::new);
		if (qrLoginRepository.responderSiPendiente(codigo, nuevoEstado, usuario.getId(), ahora()) == 0) {
			if (qrLoginRepository.buscarPorCodigo(codigo).isEmpty()) {
				throw new QrLoginNoEncontradoException();
			}
			throw new QrLoginNoVigenteException();
		}
	}

	private QrLogin buscarConSecreto(String codigo, String secreto) {
		return qrLoginRepository.buscarPorCodigo(codigo)
				.filter(qrLogin -> secretosIguales(qrLogin.getSecreto(), secreto))
				.orElseThrow(QrLoginNoEncontradoException::new);
	}

	/** Comparación en tiempo constante, para no revelar el secreto por el tiempo de respuesta. */
	private static boolean secretosIguales(String esperado, String recibido) {
		return recibido != null && MessageDigest.isEqual(
				esperado.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8));
	}

	private String tokenAleatorio() {
		byte[] bytes = new byte[BYTES_ALEATORIOS];
		aleatorio.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** La base guarda microsegundos: se trunca para comparar igual que lo persistido. */
	private Instant ahora() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}
}
