package com.toss.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class KeycloakRoleConverterTest {

    @Test
    void mapsRealmRolesToRolePrefixedAuthorities() {
        Map<String, Object> claims = Map.of(
                "realm_access", Map.of("roles", List.of("USER", "ADMIN")));

        assertThat(KeycloakRoleConverter.realmRoleAuthorities(claims))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void returnsEmptyWhenNoRealmAccess() {
        assertThat(KeycloakRoleConverter.realmRoleAuthorities(Map.of("sub", "abc"))).isEmpty();
    }

    @Test
    void returnsEmptyWhenRealmAccessHasNoRoles() {
        Map<String, Object> claims = Map.of("realm_access", Map.of());
        assertThat(KeycloakRoleConverter.realmRoleAuthorities(claims)).isEmpty();
    }
}
