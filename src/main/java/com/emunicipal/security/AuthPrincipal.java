package com.emunicipal.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public record AuthPrincipal(Long userId, String loginId, AppRole role, String principalType) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.asAuthority()));
    }
}
