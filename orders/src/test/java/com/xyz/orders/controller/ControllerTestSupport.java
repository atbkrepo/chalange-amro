package com.xyz.orders.controller;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

/**
 * JWTs that satisfy {@link com.xyz.orders.config.SecurityConfig}: authorities from {@code roles} claim.
 */
final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static RequestPostProcessor jwtWithRoles(String subject, String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("roles", List.of(roles))
                .build();
        return SecurityMockMvcRequestPostProcessors.authentication(
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                        jwt,
                        AuthorityUtils.createAuthorityList(roles)));
    }
}
