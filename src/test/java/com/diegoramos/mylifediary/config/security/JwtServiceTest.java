package com.diegoramos.mylifediary.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    @Test
    void generateAndParse_tokenContainsExpectedClaims() {
        JwtProperties props = new JwtProperties("my-super-secret-key-which-is-long-enough-012345", 3600);
        SecurityConfig securityConfig = new SecurityConfig();
        JwtEncoder encoder = securityConfig.jwtEncoder(props);
        JwtDecoder decoder = securityConfig.jwtDecoder(props);
        JwtService service = new JwtService(encoder, decoder, props);

        String token = service.generateToken("alice@example.com", "USER");
        assertNotNull(token);

        assertEquals("alice@example.com", service.extractEmail(token));
        assertTrue(service.isTokenValid(token, "alice@example.com"));
        assertFalse(service.isTokenValid(token, "bob@example.com"));
    }
}


