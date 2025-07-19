package com.example.SafeTag_BE.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authz -> authz
				.requestMatchers("/api/qrs/**").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.anyRequest().authenticated()
				)
				.csrf(csrf -> csrf
						.ignoringRequestMatchers(
								new AntPathRequestMatcher("/h2-console/**"),
								new AntPathRequestMatcher("/api/qrs/**")
						)
				)
				.headers(headers -> headers
						.addHeaderWriter(
								new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)
						)
				)
				.formLogin(form -> form
						.loginPage("/com/example/safetag/service/login")
						.defaultSuccessUrl("/")
				)
				.logout(logout -> logout
						.logoutRequestMatcher(new AntPathRequestMatcher("/com/example/safetag/service/logout"))
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
				);

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}
