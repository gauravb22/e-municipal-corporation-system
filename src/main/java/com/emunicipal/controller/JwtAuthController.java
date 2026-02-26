package com.emunicipal.controller;

import com.emunicipal.entity.StaffUser;
import com.emunicipal.entity.User;
import com.emunicipal.repository.StaffUserRepository;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.security.AppRole;
import com.emunicipal.security.AuthPrincipal;
import com.emunicipal.security.JwtTokenService;
import com.emunicipal.util.PhoneNumberUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class JwtAuthController {
    private final UserRepository userRepository;
    private final StaffUserRepository staffUserRepository;
    private final JwtTokenService jwtTokenService;

    public JwtAuthController(UserRepository userRepository,
                             StaffUserRepository staffUserRepository,
                             JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.staffUserRepository = staffUserRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request == null || request.role() == null || request.password() == null || request.password().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Role and password are required.");
        }

        AppRole requestedRole = AppRole.fromInput(request.role());
        if (requestedRole == null) {
            return error(HttpStatus.BAD_REQUEST, "Role must be ADMIN, WARD_MEMBER, or CITIZEN.");
        }

        if (requestedRole == AppRole.CITIZEN) {
            return citizenLogin(request);
        }
        return staffLogin(request, requestedRole);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody(required = false) ValidateTokenRequest request,
                                                             HttpServletRequest httpRequest) {
        String token = resolveToken(request, httpRequest);
        if (token == null) {
            return error(HttpStatus.BAD_REQUEST, "Token missing. Send it in body {\"token\":\"...\"} or Authorization header.");
        }

        if (!jwtTokenService.isValid(token)) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        AuthPrincipal principal = jwtTokenService.parseToken(token);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("valid", true);
        payload.put("userId", principal.userId());
        payload.put("loginId", principal.loginId());
        payload.put("role", principal.role().name());
        payload.put("principalType", principal.principalType());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return error(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Object principalObj = authentication.getPrincipal();
        if (principalObj instanceof AuthPrincipal principal) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", principal.userId());
            payload.put("loginId", principal.loginId());
            payload.put("role", principal.role().name());
            payload.put("principalType", principal.principalType());
            return ResponseEntity.ok(payload);
        }
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    private ResponseEntity<Map<String, Object>> citizenLogin(LoginRequest request) {
        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(request.phone());
        if (normalizedPhone == null) {
            return error(HttpStatus.BAD_REQUEST, "Valid 10-digit phone number is required for CITIZEN.");
        }

        User user = userRepository.findByPhone(normalizedPhone);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "Citizen account not found.");
        }
        if (!request.password().equals(user.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid password.");
        }
        if (user.getActive() != null && !user.getActive()) {
            return error(HttpStatus.BAD_REQUEST, "Account is blocked. Please contact administration.");
        }

        AuthPrincipal principal = new AuthPrincipal(user.getId(), normalizedPhone, AppRole.CITIZEN, "CITIZEN");
        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issueToken(principal);

        Map<String, Object> payload = buildTokenPayload(principal, issuedToken);
        payload.put("fullName", user.getFullName());
        return ResponseEntity.ok(payload);
    }

    private ResponseEntity<Map<String, Object>> staffLogin(LoginRequest request, AppRole requestedRole) {
        String username = request.username() == null ? null : request.username().trim();
        if (username == null || username.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Username is required for ADMIN and WARD_MEMBER.");
        }

        StaffUser staffUser = staffUserRepository.findByUsername(username);
        if (staffUser == null) {
            return error(HttpStatus.NOT_FOUND, "Staff account not found.");
        }
        if (!request.password().equals(staffUser.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid password.");
        }

        AppRole actualRole = AppRole.fromStaffRole(staffUser.getRole());
        if (actualRole == null || actualRole != requestedRole) {
            return error(HttpStatus.BAD_REQUEST, "Role mismatch for this account.");
        }

        AuthPrincipal principal = new AuthPrincipal(staffUser.getId(), username, actualRole, "STAFF");
        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issueToken(principal);

        Map<String, Object> payload = buildTokenPayload(principal, issuedToken);
        payload.put("fullName", staffUser.getFullName());
        return ResponseEntity.ok(payload);
    }

    private Map<String, Object> buildTokenPayload(AuthPrincipal principal, JwtTokenService.IssuedToken issuedToken) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tokenType", "Bearer");
        payload.put("accessToken", issuedToken.token());
        payload.put("expiresAt", issuedToken.expiresAt().toString());
        payload.put("expiresInMinutes", jwtTokenService.expirationMinutes());
        payload.put("userId", principal.userId());
        payload.put("loginId", principal.loginId());
        payload.put("role", principal.role().name());
        payload.put("principalType", principal.principalType());
        return payload;
    }

    private String resolveToken(ValidateTokenRequest request, HttpServletRequest httpRequest) {
        if (request != null && request.token() != null && !request.token().isBlank()) {
            return request.token().trim();
        }
        return jwtTokenService.resolveToken(httpRequest);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    public record LoginRequest(String role, String phone, String username, String password) {
    }

    public record ValidateTokenRequest(String token) {
    }
}
