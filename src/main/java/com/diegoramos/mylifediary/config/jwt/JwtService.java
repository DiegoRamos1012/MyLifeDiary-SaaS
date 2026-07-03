package com.diegoramos.mylifediary.config.jwt;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder jwtEncoder,
                      JwtDecoder jwtDecoder,
                      JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    public String generateToken(String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getExpirationSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mylifediary")
                .issuedAt(now)
                .expiresAt(exp)
                .subject(email)
                .claim("role", role)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String extractEmail(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getSubject();
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return expectedEmail.equalsIgnoreCase(jwt.getSubject()) && jwt.getExpiresAt() != null && jwt.getExpiresAt().isAfter(Instant.now());
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return properties.getExpirationSeconds();
    }
}




