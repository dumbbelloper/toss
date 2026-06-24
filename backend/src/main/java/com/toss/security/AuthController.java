package com.toss.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증된 현재 사용자 정보.
 *
 * <p>두 종류의 인증을 모두 받는다: 웹 BFF 는 세션 기반 {@link OidcUser}(ID token claims),
 * 모바일은 Bearer {@link Jwt}(access token claims). 클레임 키는 동일하므로 공통 추출한다.
 * SPA·앱은 부팅 시 이 엔드포인트로 로그인 여부를 확인한다(200 → 로그인됨, 401 → 필요).
 */
@RestController
public class AuthController {

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> claims = claimsOf(authentication.getPrincipal());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", claims.get("sub"));
        body.put("username", claims.get("preferred_username"));
        body.put("email", claims.get("email"));
        body.put("name", claims.get("name"));
        body.put("roles", realmRoles(claims));
        return body;
    }

    /** OidcUser(웹 세션) / Jwt(모바일 Bearer) 어느 쪽이든 클레임 맵으로 정규화. */
    private Map<String, Object> claimsOf(Object principal) {
        if (principal instanceof OidcUser oidc) {
            return oidc.getClaims();
        }
        if (principal instanceof Jwt jwt) {
            return jwt.getClaims();
        }
        return Map.of();
    }

    /** Keycloak realm_access.roles 추출 (없으면 빈 목록). */
    private Object realmRoles(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") != null) {
            return map.get("roles");
        }
        return List.of();
    }
}
