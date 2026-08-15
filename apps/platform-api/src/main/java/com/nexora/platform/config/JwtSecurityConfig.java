package com.nexora.platform.config;

import com.nexora.platform.auth.DualJwtDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * M6-R01: JWT security configuration with Ed25519 migration support.
 *
 * <p>Uses {@link DualJwtDecoder} to support both Ed25519 (primary) and
 * HS256 (legacy fallback) during the three-phase migration.</p>
 */
@Configuration
@EnableWebSecurity
public class JwtSecurityConfig {

    @Value("${nexora.auth.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${nexora.auth.legacy-secret:}")
    private String legacySecret;

    @Value("${nexora.auth.legacy-hs256-fallback:false}")
    private boolean legacyFallbackEnabled;

    @Bean
    public JwtDecoder jwtDecoder() {
        return DualJwtDecoder.fromJwkSetUri(jwkSetUri, legacySecret, legacyFallbackEnabled);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/platform/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }
}
