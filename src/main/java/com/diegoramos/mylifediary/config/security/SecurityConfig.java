package com.diegoramos.mylifediary.config.security;

import com.diegoramos.mylifediary.common.response.ApiErrorResponse;
import com.diegoramos.mylifediary.config.jwt.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static void writeSecurityError(HttpServletResponse response,
                                           ObjectMapper objectMapper,
                                           int status,
                                           String error,
                                           String message,
                                           String path) throws java.io.IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                List.of()
        );

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper objectMapper) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/verify-email").permitAll()
                        // Admin-only
                        .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/users/*/reactivate").hasRole("ADMIN")
                        // Authenticated for user operations
                        .requestMatchers(HttpMethod.PATCH, "/users/*/userInfo").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/*/changeEmail").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/*/changePassword").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/*/deactivate").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, ignored) ->
                                writeSecurityError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized", "Token ausente, inválido ou expirado", request.getRequestURI()))
                        .accessDeniedHandler((request, response, ignored) ->
                                writeSecurityError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden", "Acesso não permitido", request.getRequestURI()))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        byte[] secret = properties.getSecret() == null ? new byte[0] : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        byte[] secret = properties.getSecret() == null ? new byte[0] : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }
}
