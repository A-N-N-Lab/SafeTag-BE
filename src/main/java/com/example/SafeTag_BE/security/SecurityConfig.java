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
				.cors(cors -> {}) // 아래 corsConfigurationSource() 사용
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Preflight
						.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

						// 문서/개발 도구
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/h2-console/**", "/error").permitAll()

						// 공개 인증/회원 경로 (필요 시만 유지)
						.requestMatchers("/api/auth/login", "/api/user/signup", "/api/admin/signup").permitAll()

						// SafeTag 기존 공개 엔드포인트(유지 시)
						.requestMatchers("/api/relay/**", "/c/**", "/voice/**", "/actuator/health").permitAll()

						// QR/Chat 공개
						.requestMatchers("/api/qrs/**", "/api/chat/**").permitAll()

						// WebRTC 세션 발급 & ICE 설정
						.requestMatchers("/api/calls/**", "/api/ice-config").permitAll()

						// WebSocket 시그널링
						.requestMatchers("/ws/**").permitAll()

						// 그 외는 인증 필요
						.anyRequest().authenticated()
				)
				.headers(headers -> headers
						// H2 콘솔용
						.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
				)
				// JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// 로컬/사설망/터널을 폭넓게 허용 (운영에서는 꼭 좁히세요)
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration c = new CorsConfiguration();
		c.setAllowedOriginPatterns(List.of(
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
		c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
		c.setAllowedHeaders(List.of("Authorization","Content-Type"));
		c.setExposedHeaders(List.of("Authorization"));
		c.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
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
