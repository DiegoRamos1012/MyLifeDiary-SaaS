package com.diegoramos.mylifediary.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JwtToCustomUserDetailsConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        UUID userId = UUID.fromString(
                jwt.getClaimAsString("userId")
        );

        String email = jwt.getSubject();

        String role = jwt.getClaimAsString("role");

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(() -> "ROLE_" + role);

        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                email,
                null,
                authorities
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                jwt,
                authorities
        );
    }
}