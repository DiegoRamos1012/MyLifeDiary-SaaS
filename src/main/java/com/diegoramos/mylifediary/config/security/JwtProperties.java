package com.diegoramos.mylifediary.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:3600}")
    private long expirationSeconds;

    @Value("${jwt.refresh-token.expiration-days:7}")
    private long refreshTokenExpirationDays;

    public String getSecret() {
        return secret;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    // Public constructor to ease testing and manual instantiation
    public JwtProperties(String secret, long expirationSeconds) {
        this(secret, expirationSeconds, 7L);
    }

    public JwtProperties(String secret, long expirationSeconds, long refreshTokenExpirationDays) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public JwtProperties() {
    }
}


