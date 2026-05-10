package com.diegoramos.mylifediary.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:3600}")
    private long expirationSeconds;

    public String getSecret() {
        return secret;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    // Public constructor to ease testing and manual instantiation
    public JwtProperties(String secret, long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public JwtProperties() {
    }
}


