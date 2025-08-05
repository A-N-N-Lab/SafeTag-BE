package com.example.SafeTag_BE.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String SECRET_KEY = "safetagSecretKeyForJwtAuthentication";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1시간

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    //JWT 생성
    public String generateToken(Long id, String role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(id));
        claims.put("role", role); // "ROLE_USER" or "ROLE_ADMIN"

        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    //JWT 유효성 검증
    public boolean validateToken(String token) {
        try {
            getParser().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("Invalid JWT signature or claims: {}", e.getMessage());
        }
        return false;
    }

    //JWT에서 사용자 ID(회원번호)추출
    public Long getUserIdFromToken(String token) {
        String subject = getParser()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        return Long.parseLong(subject);
    }

    //JWT에서 역할 추출
    public String getRoleFromToken(String token) {
        return getParser()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class); // "ROLE_USER" or "ROLE_ADMIN"
    }

    //공통 ParserBuilder
    private JwtParser getParser() {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build();
    }

    public String getUsernameFromToken(String token) {
        return getParser()
                .parseClaimsJws(token)
                .getBody()
                .get("username", String.class);
    }

}
