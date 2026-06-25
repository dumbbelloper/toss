# ADR 0002 — 인가(Authorization) 정책

- 상태: 채택
- 날짜: 2026-06-25

## 맥락
인증(누구인가)은 3-플랫폼(web BFF / Android / iOS) 모두 완료됐다. 인가(무엇을 할 수 있나)는
없었다 — 두 SecurityFilterChain 모두 `authenticated()` 뿐이고, Keycloak realm 역할(USER/ADMIN)이
Spring authority 로 매핑되지 않아 `hasRole`/`@PreAuthorize` 가 동작하지 않았다. `/api/me` 의 roles 는
표시용으로 클레임을 직접 읽을 뿐이었다.

제품은 현재 **1인 사용자**(멀티테넌시 보류, `docs/PRODUCT.md`). 정교한 RBAC 는 시기상조다.

## 결정
**정교한 역할 게이트를 만들지 않는다. 대신 올바른 토대만 깐다.**

1. Keycloak `realm_access.roles` → Spring `ROLE_*` authority 매핑을 **양 체인**에 적용
   (`KeycloakRoleConverter`): 모바일은 `JwtAuthenticationConverter`(scope `SCOPE_*` + 역할 `ROLE_*`),
   웹은 `GrantedAuthoritiesMapper`(OIDC principal 에 `ROLE_*` 추가).
2. `@EnableMethodSecurity` 활성화 — 필요 시 메서드에 `@PreAuthorize` 한 줄로 게이트.
3. **현재 정책: `/api/**` = 인증된 사용자 전체 허용.** 역할로 막는 엔드포인트는 아직 없음.
4. `ADMIN` 역할은 **향후 운영/관리 기능**(데이터 적재 잡 제어, 멀티유저 전환 시 관리)용으로 예약.

## 결과
- 지금 동작 차이는 없음(아무것도 역할로 막지 않음) — 그러나 역할이 first-class authority 가 되어
  미래의 게이트가 한 줄로 가능.
- 가짜 ADMIN 엔드포인트 같은 노이즈를 만들지 않음.
- 검증: `KeycloakRoleConverterTest`(realm 역할 → ROLE_* 3 케이스) + 새 설정으로 backend 정상 부팅.

## 트레이드오프
- 소비처가 아직 없는 매핑을 미리 깐다(약한 YAGNI 위반)지만, 매핑 없이는 인가 자체가 불가능하므로
  "인가의 최소 실체"로 본다.
