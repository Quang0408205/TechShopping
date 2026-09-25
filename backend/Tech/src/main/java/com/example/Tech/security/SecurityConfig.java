package com.example.Tech.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless JWT security (OAuth2 resource server, see JwtConfig).
 * <ul>
 *     <li>Auth endpoints and Swagger are public.</li>
 *     <li>Product catalogue (8 resources): GET is public, POST/PUT/PATCH/DELETE require ADMIN.</li>
 *     <li>/api/v1/admin/** requires ADMIN; any other /api/v1/** requires a valid access token.</li>
 *     <li>Everything else is denied.</li>
 * </ul>
 * A request that sends an invalid bearer token is rejected with 401 even on public endpoints.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    static final String ADMIN = "ADMIN";

    /** Product catalogue resources: public reads, ADMIN-only writes (decision D4). */
    static final String[] CATALOG_PATHS = {
            "/api/v1/categories/**",
            "/api/v1/brands/**",
            "/api/v1/products/**",
            "/api/v1/product-variants/**",
            "/api/v1/attributes/**",
            "/api/v1/attribute-values/**",
            "/api/v1/product-images/**",
            "/api/v1/product-specifications/**"
    };

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, CATALOG_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, CATALOG_PATHS).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, CATALOG_PATHS).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, CATALOG_PATHS).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, CATALOG_PATHS).hasRole(ADMIN)
                        .requestMatchers("/api/v1/admin/**").hasRole(ADMIN)
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
