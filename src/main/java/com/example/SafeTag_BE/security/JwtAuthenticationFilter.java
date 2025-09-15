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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtTokenProvider.validateToken(token)) {
                //JWT에서 추출
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                String uname = jwtTokenProvider.getUsernameFromToken(token); // 토큰에 username 있으면 추출

                //사용자 정보 조회
                UserDetails delegate = userSecurityService.loadUserById(userId);

                //role 기반 인증 객체 생성
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

                //인증 정보 SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private static final class AuthPrincipal implements org.springframework.security.core.userdetails.UserDetails {

        private final Long id;
        private final String username; // 토큰에 없으면 delegate의 username으로 fallback
        private final String role;
        private final UserDetails delegate;

        private AuthPrincipal(Long id,
                              String username,
                              String role,
                              UserDetails delegate) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.delegate = delegate;
        }

        public Long getId() { return id; }
        public String getRole() { return role; }

        @Override
        public String getPassword() { return delegate.getPassword(); }

        @Override
        public String getUsername() {
            return (username != null ? username : delegate.getUsername());
        }

        @Override
        public boolean isAccountNonExpired()  { return delegate.isAccountNonExpired(); }
        @Override
        public boolean isAccountNonLocked()   { return delegate.isAccountNonLocked(); }
        @Override
        public boolean isCredentialsNonExpired(){ return delegate.isCredentialsNonExpired(); }
        @Override
        public boolean isEnabled()             { return delegate.isEnabled(); }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return delegate.getAuthorities();
        }
    }
}
