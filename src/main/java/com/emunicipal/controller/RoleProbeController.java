package com.emunicipal.controller;

import com.emunicipal.security.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoleProbeController {

    @GetMapping("/admin/ping")
    public Map<String, Object> adminPing(Authentication authentication) {
        return buildPayload("ADMIN", authentication);
    }

    @GetMapping("/ward/ping")
    public Map<String, Object> wardPing(Authentication authentication) {
        return buildPayload("WARD_MEMBER", authentication);
    }

    @GetMapping("/citizen/ping")
    public Map<String, Object> citizenPing(Authentication authentication) {
        return buildPayload("CITIZEN", authentication);
    }

    private Map<String, Object> buildPayload(String endpointRole, Authentication authentication) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", true);
        payload.put("endpointRole", endpointRole);

        Object principalObj = authentication == null ? null : authentication.getPrincipal();
        if (principalObj instanceof AuthPrincipal principal) {
            payload.put("userId", principal.userId());
            payload.put("loginId", principal.loginId());
            payload.put("tokenRole", principal.role().name());
        }
        return payload;
    }
}
