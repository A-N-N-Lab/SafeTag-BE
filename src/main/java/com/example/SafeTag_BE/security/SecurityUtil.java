package com.example.SafeTag_BE.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

public final class SecurityUtil {

    private SecurityUtil() {}

    // 로그인 사용자의 id를 반환
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }

        Object principal = auth.getPrincipal();

        // 우리 프로젝트 표준 커스텀 프린시펄을 쓰는 경우 (있다면)
        if (principal instanceof CustomUserPrincipal p) {
            return p.id();
        }

        // JwtAuthenticationFilter.AuthPrincipal (private inner class) 리플렉션으로 getId() 호출
        Long reflectedId = tryExtractIdByReflection(principal);
        if (reflectedId != null) {
            return reflectedId;
        }


        throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
    }

    // 관리자면 id
    public static Long getCurrentUserIdOrNullIfAdmin() {
        return hasRole("ROLE_ADMIN") ? null : getCurrentUserId();
    }

    // 현재 사용자가 관리자면 id 반환
    public static Long getCurrentAdminId() {
        return hasRole("ROLE_ADMIN") ? getCurrentUserId() : null;
    }

    // 권한 보유 여부
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }


    private static Long tryExtractIdByReflection(Object principal) {
        if (principal == null) return null;
        try {
            Method m;
            try {
                m = principal.getClass().getDeclaredMethod("getId");
            } catch (NoSuchMethodException e) {
                m = principal.getClass().getMethod("getId");
            }
            m.setAccessible(true);
            Object val = m.invoke(principal);
            if (val instanceof Long) return (Long) val;
            if (val instanceof Number) return ((Number) val).longValue();
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
