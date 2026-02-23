package com.emunicipal.config;

import com.emunicipal.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/login",
                                "/check-phone",
                                "/citizen-password-login",
                                "/otp-page",
                                "/verify-otp",
                                "/register",
                                "/language",
                                "/works-yojna",
                                "/ward-login",
                                "/admin-login",
                                "/api/auth/login",
                                "/api/auth/validate",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/ward/**").hasAnyRole("WARD_MEMBER", "ADMIN")
                        .requestMatchers("/api/citizen/**").hasRole("CITIZEN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/ward-dashboard",
                                "/ward-profile",
                                "/ward-profile/**",
                                "/ward-complaints/**",
                                "/ward-works/create",
                                "/ward-works/*/edit",
                                "/ward-works/*/delete"
                        ).hasRole("WARD_MEMBER")
                        .requestMatchers(
                                "/dashboard",
                                "/profile",
                                "/profile/**",
                                "/my-profile",
                                "/update-profile",
                                "/raise-complaint",
                                "/complaint-form",
                                "/submit-complaint",
                                "/complaint-status",
                                "/escalate-complaint/**",
                                "/submit-feedback/**",
                                "/citizen/**"
                        ).hasRole("CITIZEN")
                        .requestMatchers(
                                "/ward-works",
                                "/ward-works-citizen",
                                "/api/ward-works",
                                "/ward-works/*/like",
                                "/ward-works/*/comment",
                                "/ward-works/*/rate"
                        ).hasAnyRole("CITIZEN", "WARD_MEMBER", "ADMIN")
                        .requestMatchers("/logout").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\"}");
                                return;
                            }
                            if (request.getRequestURI().startsWith("/admin")) {
                                response.sendRedirect("/admin-login");
                                return;
                            }
                            if (request.getRequestURI().startsWith("/ward")) {
                                response.sendRedirect("/ward-login");
                                return;
                            }
                            response.sendRedirect("/login");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Forbidden\"}");
                                return;
                            }
                            response.sendRedirect("/login");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
