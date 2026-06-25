package com.toss.security;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Keycloak {@code realm_access.roles} 클레임을 Spring Security {@code ROLE_*} authority 로 변환한다.
 *
 * <p>웹(BFF, ID token claims)과 모바일(Bearer, access token claims) 어느 쪽이든 클레임 구조가 같아
 * 공통으로 쓴다. 이 매핑이 있어야 {@code hasRole("ADMIN")} / {@code @PreAuthorize} 가 동작한다.
 */
final class KeycloakRoleConverter {

    private KeycloakRoleConverter() {
    }

    /** {@code realm_access.roles} → {@code ROLE_<role>} authority 집합. 없으면 빈 집합. */
    static Set<GrantedAuthority> realmRoleAuthorities(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            return roles.stream()
                    .map(Object::toString)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }
}
