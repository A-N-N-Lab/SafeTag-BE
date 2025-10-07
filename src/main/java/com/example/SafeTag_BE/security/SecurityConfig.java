package com.example.SafeTag_BE.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	// application.yml의 app.allowed-origins 리스트를 읽음(없으면 빈 리스트)
	@Value("${app.allowed-origins:}")
	private List<String> allowedOriginsFromProp;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> {}) // 아래 corsConfigurationSource() 사용
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/h2-console/**", "/error").permitAll()
						.requestMatchers("/api/auth/login", "/api/user/signup", "/api/admin/signup").permitAll()
						.requestMatchers("/api/relay/**", "/c/**", "/voice/**", "/actuator/health").permitAll()
						// QR: 발급/회전은 인증 필요, 그 외는 기존처럼 공개
						.requestMatchers("/api/qrs/issue-or-rotate").authenticated()
						.requestMatchers("/api/qrs/**").permitAll()
						.requestMatchers("/api/chat/**").permitAll()
						.requestMatchers("/api/calls/**", "/api/ice-config").permitAll()
						.requestMatchers("/ws/**").permitAll()
						.anyRequest().authenticated()
				)
				.headers(headers -> headers
						.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// CORS 설정: app.allowed-origins 사용(없으면 기본 패턴), 응답 헤더 'Expires-At' 노출
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration c = new CorsConfiguration();

		List<String> origins = new ArrayList<>();
		if (allowedOriginsFromProp != null && !allowedOriginsFromProp.isEmpty()) {
			origins.addAll(allowedOriginsFromProp);
			c.setAllowedOrigins(origins); // 정확 매칭
		} else {
			c.setAllowedOriginPatterns(List.of(
					"http://localhost:*",
					"https://localhost:*",
					"http://127.0.0.1:*",
					"https://127.0.0.1:*",
					"http://192.168.*:*",
					"http://10.*:*",
					"http://172.*:*",
					"https://*.ngrok.io",
					"https://*.ngrok-free.app",
					"https://*.trycloudflare.com"
			));
		}

		c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		c.setExposedHeaders(List.of("Authorization", "Expires-At")); // Expires-At 노출
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
