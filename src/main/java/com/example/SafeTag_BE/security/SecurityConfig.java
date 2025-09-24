//package com.example.SafeTag_BE.security;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//	private final JwtAuthenticationFilter jwtAuthenticationFilter;
//
//	@Bean
//	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//		http
//				.csrf(csrf -> csrf.disable())
//				.cors(cors -> {})
//				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//				.authorizeHttpRequests(auth -> auth
//						.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
//						.requestMatchers(
//								"/api/auth/login",
//								"/api/user/signup",
//								"/api/admin/signup",
//								"/swagger-ui/**",
//								"/swagger-ui.html",
//								"/v3/api-docs/**",
//								"/h2-console/**",
//								"/error"                     // [ADD] 스프링 기본 에러 페이지 접근 허용
//						).permitAll()
//						// === 중계 서버용 공개 엔드포인트 ===
//						.requestMatchers("/api/relay/**").permitAll()
//						.requestMatchers("/c/**").permitAll()             // [ADD] QR 랜딩(Thymeleaf/정적)
//						.requestMatchers("/voice/**").permitAll()         // [ADD] 통신사/공급사 Webhook (POST 포함)
//						.requestMatchers("/actuator/health").permitAll()  // [ADD] 헬스체크(선택)
//						// ===============================
//						.anyRequest().authenticated()
//				)
//				.headers(headers -> headers
//						.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
//				)
//				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//
//		return http.build();
//	}
//
//	// 로컬/모바일 테스트 친화 CORS 설정
//	@Bean
//	public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
//		var c = new org.springframework.web.cors.CorsConfiguration();
//
//		// 개발 편의를 위해 패턴 기반 허용(로컬/사설망/프록시 터널 포함)
//		// 필요시 운영에서는 꼭 좁혀 주세요.
//		c.setAllowedOriginPatterns(java.util.List.of(              // [ADD] 패턴 허용
//				"http://localhost:*",
//				"http://127.0.0.1:*",
//				"http://192.168.*:*",
//				"http://10.*:*",
//				"http://172.*:*",
//				"https://*.ngrok.io",
//				"https://*.trycloudflare.com"
//		));
//		// ※ 기존 setAllowedOrigins 대신 setAllowedOriginPatterns 사용
//
//		c.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
//		c.setAllowedHeaders(java.util.List.of("Authorization","Content-Type"));
//		c.setExposedHeaders(java.util.List.of("Authorization"));
//		c.setAllowCredentials(true);
//
//		var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", c);
//		return source;
//	}
//
//	@Bean
//	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
//	}
//
//	@Bean
//	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
//		return configuration.getAuthenticationManager();
//	}
//}
package com.example.SafeTag_BE.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> {}) // 아래 corsConfigurationSource() 사용
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

						// ---- 공개 인증/문서 경로 ----
						.requestMatchers(
								"/api/auth/login",
								"/api/user/signup",
								"/api/admin/signup",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**",
								"/h2-console/**",
								"/error"
						).permitAll()

						// ---- SafeTag: 기존 프록시/랜딩/웹훅 (유지 시) ----
						.requestMatchers("/api/relay/**").permitAll()
						.requestMatchers("/c/**").permitAll()
						.requestMatchers("/voice/**").permitAll()
						.requestMatchers("/actuator/health").permitAll()

						// ---- WebRTC: 세션 발급 + WS 시그널링 ----
						.requestMatchers("/api/calls/**").permitAll()
						.requestMatchers("/api/ice-config").permitAll()

						.requestMatchers("/ws/**").permitAll()

						.anyRequest().authenticated()
				)
				.headers(headers -> headers
						.addHeaderWriter(new XFrameOptionsHeaderWriter(
								XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)) // h2-console
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// 로컬/모바일/터널까지 넓게 허용(운영 시 좁히세요)
	@Bean
	public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
		var c = new org.springframework.web.cors.CorsConfiguration();
		c.setAllowedOriginPatterns(java.util.List.of(
				"http://localhost:*",
				"https://localhost:*",
				"http://127.0.0.1:*",
				"https://127.0.0.1:*",
				"http://192.168.*:*",
				"http://10.*:*",
				"http://172.*:*",
				"https://*.ngrok.io",
				"https://*.trycloudflare.com"
		));
		c.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
		c.setAllowedHeaders(java.util.List.of("Authorization","Content-Type"));
		c.setExposedHeaders(java.util.List.of("Authorization"));
		c.setAllowCredentials(true);

		var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", c);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
