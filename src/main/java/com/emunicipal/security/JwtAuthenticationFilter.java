package com.emunicipal.security;

import com.emunicipal.entity.StaffUser;
import com.emunicipal.entity.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Authentication authentication = authenticationFromJwt(request);
            if (authentication == null) {
                authentication = authenticationFromSession(request);
            }
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private Authentication authenticationFromJwt(HttpServletRequest request) {
        String token = jwtTokenService.resolveToken(request);
        if (token == null) {
            return null;
        }

        try {
            AuthPrincipal principal = jwtTokenService.parseToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            return authentication;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private Authentication authenticationFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object userObj = session.getAttribute("user");
        if (userObj instanceof User user) {
            if (user.getId() == null || (user.getActive() != null && !user.getActive())) {
                return null;
            }
            String loginId = user.getPhone() == null ? "citizen-" + user.getId() : user.getPhone();
            return buildAuthentication(new AuthPrincipal(user.getId(), loginId, AppRole.CITIZEN, "CITIZEN_SESSION"), request);
        }

        Object staffObj = session.getAttribute("staffUser");
        if (staffObj instanceof StaffUser staffUser) {
            AppRole role = AppRole.fromStaffRole(staffUser.getRole());
            if (role == null || staffUser.getId() == null) {
                return null;
            }
            String loginId = staffUser.getUsername() == null ? "staff-" + staffUser.getId() : staffUser.getUsername();
            return buildAuthentication(new AuthPrincipal(staffUser.getId(), loginId, role, "STAFF_SESSION"), request);
        }

        return null;
    }

    private Authentication buildAuthentication(AuthPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }
}
