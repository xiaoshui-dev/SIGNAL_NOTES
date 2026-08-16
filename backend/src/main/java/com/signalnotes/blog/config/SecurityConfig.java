package com.signalnotes.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' data: https://fonts.gstatic.com; script-src 'self'; connect-src 'self' http://127.0.0.1:* http://localhost:*; object-src 'none'; base-uri 'self'; frame-ancestors 'none'"))
                .permissionsPolicyHeader(policy -> policy.policy("camera=(), microphone=(), geolocation=()")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/users/**", "/api/admin/settings", "/api/admin/email/**", "/api/admin/logs/**", "/api/admin/backups/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/contact-messages/**", "/api/admin/subscriptions/**", "/api/admin/comments/**").hasAnyRole("EDITOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/posts/**").hasAnyRole("EDITOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/posts/batch").hasAnyRole("EDITOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/posts/*/restore", "/api/admin/posts/*/revisions/*/restore").hasAnyRole("EDITOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/posts").hasAnyRole("AUTHOR", "EDITOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/posts/*").hasAnyRole("AUTHOR", "EDITOR", "ADMIN")
                .requestMatchers("/api/admin/media/**").hasAnyRole("AUTHOR", "EDITOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/categories/**", "/api/admin/tags/**").hasAnyRole("VIEWER", "AUTHOR", "EDITOR", "ADMIN")
                .requestMatchers("/api/admin/categories/**", "/api/admin/tags/**").hasAnyRole("EDITOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/dashboard", "/api/admin/me", "/api/admin/posts/**").hasAnyRole("VIEWER", "AUTHOR", "EDITOR", "ADMIN")
                .requestMatchers("/api/admin/account/**").authenticated()
                .requestMatchers("/api/admin/**").denyAll()
                .requestMatchers("/actuator/health", "/api/**", "/uploads/**").permitAll()
                .anyRequest().permitAll())
            .httpBasic(Customizer.withDefaults()).build();
    }

    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "Accept"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
