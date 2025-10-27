//package com.example.SafeTag_BE.security;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
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
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import jakarta.servlet.http.HttpServletResponse;
//import java.util.ArrayList;
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//	private final JwtAuthenticationFilter jwtAuthenticationFilter;
//
//	// application.yml의 app.allowed-origins 읽기
//	@Value("${app.allowed-origins:}")
//	private List<String> allowedOriginsFromProp;
//
//	@Bean
//	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//		http
//				//  기본 설정
//				.csrf(csrf -> csrf.disable())
//				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
//				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//
//				//  요청 권한 설정
//				.authorizeHttpRequests(auth -> auth
//						// swagger & h2-console 등
//						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
//						.requestMatchers("/h2-console/**", "/error").permitAll()
//
//						// 로그인/회원가입/회원관리
//						.requestMatchers("/api/auth/**").permitAll()
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//						.requestMatchers("/api/user/signup", "/api/admin/signup").permitAll()
//
//						// relay / call / chat 관련
//						.requestMatchers("/api/relay/**", "/api/calls/**", "/api/ice-config", "/api/chat/**").permitAll()
//
//						// QR 관련: issue-or-rotate은 인증 없어야 라즈베리파이에서 발급 가능
//						.requestMatchers(HttpMethod.POST, "/api/qrs/issue-or-rotate").permitAll()
//						.requestMatchers("/api/qrs/**", "/ws/**", "/actuator/health").permitAll()
//
//
//						// 나머지는 인증 필요
//						.anyRequest().authenticated()
//				)
//
//				// 헤더 보안 설정
//				.headers(headers -> headers
//						.addHeaderWriter(new XFrameOptionsHeaderWriter(
//								XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
//				)
//
//				//  JWT 필터 등록
//				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//
//				// BasicAuth 끄기 (팝업 방지)
//				.httpBasic(httpBasic -> httpBasic.disable())
//
//				// 401 응답 시 JSON 반환
//				.exceptionHandling(ex -> ex
//						.authenticationEntryPoint((req,res,e)-> { /* 401 JSON */ })
//						.accessDeniedHandler((req,res,e)-> {
//							res.setStatus(HttpServletResponse.SC_FORBIDDEN);
//							res.setContentType("application/json;charset=UTF-8");
//							res.getWriter().write("{\"error\":\"FORBIDDEN\"}");
//						})
//				);
//
//		return http.build();
//	}
//
//	//  CORS 설정
//	@Bean
//	public CorsConfigurationSource corsConfigurationSource() {
//		CorsConfiguration c = new CorsConfiguration();
//
//		List<String> origins = new ArrayList<>();
//		if (allowedOriginsFromProp != null && !allowedOriginsFromProp.isEmpty()) {
//			origins.addAll(allowedOriginsFromProp);
//			c.setAllowedOrigins(origins);
//		} else {
//			c.setAllowedOriginPatterns(List.of(
////					"http://localhost:*",
////					"https://localhost:*",
////					"http://127.0.0.1:*",
////					"https://127.0.0.1:*",
////					"http://192.168.*:*",
////					"http://172.*:*",
////					"http://10.*:*",
////					"https://*.ngrok.io",
////					"https://*.ngrok-free.app",
//					"https://*.trycloudflare.com",
//					"https://grammatical-krystle-unimpressively.ngrok-free.app",
//					"https://grammatical-krystle-unimpressively.ngrok-free.dev",
//					"http://localhost:5173",
//					"http://172.20.10.5:5173"
//
//			));
//		}
//
////		c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
////		c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
////		c.setExposedHeaders(List.of("Authorization", "Expires-At"));
////		c.setAllowCredentials(true);
//		c.addAllowedMethod("*");
//		c.addAllowedHeader("*");
//		c.setAllowCredentials(true);
//		c.setExposedHeaders(List.of("Authorization","Expires-At"));
//
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", c);
//		return source;
//	}
//
//	// ===== 비밀번호 인코더 =====
//	@Bean
//	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
//	}
//
//	// ===== AuthenticationManager (로그인 시 사용) =====
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
				.cors(cors -> {}) // 아래 Bean 사용
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Preflight
						.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

						// 문서/개발 도구
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/h2-console/**", "/error").permitAll()

						// 공개 인증/회원 경로
						.requestMatchers("/api/auth/**", "/api/user/signup", "/api/admin/signup").permitAll()

						// 기존 공개 엔드포인트
						.requestMatchers("/api/relay/**", "/c/**", "/voice/**", "/actuator/health").permitAll()

						// QR/Chat 공개
						.requestMatchers("/api/qrs/**", "/api/chat/**").permitAll()

						// WebRTC
						.requestMatchers("/api/calls/**", "/api/ice-config").permitAll()

						// WebSocket
						.requestMatchers("/ws/**").permitAll()

						// 스티커 발급: 인증 필요
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/sticker/issue").authenticated()

						// 마이페이지
						.requestMatchers("/api/mypage/**").hasAnyRole("USER", "ADMIN")

						// 그 외
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((req, res, e) -> {
							res.setStatus(401);
							res.setContentType("application/json;charset=UTF-8");
							res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + e.getMessage() + "\"}");
						})
						.accessDeniedHandler((req, res, e) -> {
							res.setStatus(403);
							res.setContentType("application/json;charset=UTF-8");
							res.getWriter().write("{\"error\":\"forbidden\",\"message\":\"" + e.getMessage() + "\"}");
						})
				)
				.headers(headers -> headers
						.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration c = new CorsConfiguration();
		c.setAllowedOriginPatterns(List.of(
				"http://localhost:*", "https://localhost:*",
				"http://127.0.0.1:*", "https://127.0.0.1:*",
				"http://192.168.*:*", "http://10.*:*", "http://172.*:*",
				"https://*.ngrok.io", "https://*.trycloudflare.com"
		));
		c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
		c.setAllowedHeaders(List.of("Authorization","Content-Type"));
		c.setExposedHeaders(List.of("Authorization","Expires-At"));
		c.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", c);
		return source;
	}

	@Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
