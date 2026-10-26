package app.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import app.usuario.UsuarioOidcService;

/**
 * Login con Google mediante Spring Security (ADR-0030).
 * <p>
 * Todas las rutas siguen siendo públicas: el camino anónimo de la Etapa 1 no cambia. Los endpoints
 * que necesitan sesión la comprueban en su controller y responden 401 con ProblemDetail.
 */
@Configuration
public class SeguridadConfig {

	@Bean
	SecurityFilterChain seguridad(HttpSecurity http, UsuarioOidcService usuarioOidcService) throws Exception {
		http
				.authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
				.oauth2Login(login -> login
						.userInfoEndpoint(datos -> datos.oidcUserService(usuarioOidcService))
						.defaultSuccessUrl("/", true)
						// login fallido o cancelado en Google: vuelve a la web sin sesión y con un mensaje
						.failureUrl("/?login=error"))
				.logout(logout -> logout
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
				// La web lee la cookie XSRF-TOKEN y la reenvía en X-XSRF-TOKEN. El alta anónima de enlaces no
				// usa la sesión y la consume también la extensión, que no tiene esa cookie.
				.csrf(csrf -> csrf.spa().ignoringRequestMatchers("/api/v1/links", "/api/v1/links/**"));
		return http.build();
	}
}
