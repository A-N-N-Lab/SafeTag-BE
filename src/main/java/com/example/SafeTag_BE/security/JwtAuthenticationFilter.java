package com.example.SafeTag_BE.security;

import com.example.SafeTag_BE.service.UserSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserSecurityService userSecurityService;

    // Swagger/H2/공개 경로는 필터 자체를 건너뛰게 함
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        return uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/swagger-ui.html")
                || uri.startsWith("/h2-console")
                || uri.startsWith("/api/qrs")
                || uri.startsWith("/api/chat")
                || uri.startsWith("/api/calls")
                || uri.startsWith("/api/ice-config")
                || uri.startsWith("/ws")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    String role = jwtTokenProvider.getRoleFromToken(token);
                    String uname = jwtTokenProvider.getUsernameFromToken(token);

                    UserDetails delegate = userSecurityService.loadUserById(userId);

                    AuthPrincipal principal = new AuthPrincipal(
                            userId,
                            (uname != null ? uname : delegate.getUsername()),
                            role,
                            delegate
                    );

                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(role))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignore) {
            // 토큰 파싱/검증 실패 시에도 예외 던지지 않음 (permitAll 경로 보호)
        }

        filterChain.doFilter(request, response);
    }

    // 사용자 정보 래퍼 (필요 시 그대로 유지)
    private static final class AuthPrincipal implements org.springframework.security.core.userdetails.UserDetails {
        private final Long id;
        private final String username;
        private final String role;
        private final UserDetails delegate;

        private AuthPrincipal(Long id, String username, String role, UserDetails delegate) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.delegate = delegate;
        }

        public Long getId() { return id; }
        public String getRole() { return role; }

        @Override public String getPassword() { return delegate.getPassword(); }
        @Override public String getUsername() { return (username != null ? username : delegate.getUsername()); }
        @Override public boolean isAccountNonExpired()  { return delegate.isAccountNonExpired(); }
        @Override public boolean isAccountNonLocked()   { return delegate.isAccountNonLocked(); }
        @Override public boolean isCredentialsNonExpired(){ return delegate.isCredentialsNonExpired(); }
        @Override public boolean isEnabled()             { return delegate.isEnabled(); }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return delegate.getAuthorities(); }
    }
}
