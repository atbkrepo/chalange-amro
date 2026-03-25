package com.xyz.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;

/**
 * Puts Spring Security user roles into access-token JWTs so resource servers can map them to
 * {@link org.springframework.security.core.GrantedAuthority} (not only OAuth2 {@code scope}).
 */
@Configuration
public class JwtAccessTokenRolesCustomizer {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> accessTokenRolesCustomizer() {
        return context -> {
            if (!"access_token".equals(context.getTokenType().getValue())) {
                return;
            }
            Authentication authentication = context.getPrincipal();
            if (authentication == null) {
                return;
            }
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof UserDetails userDetails)) {
                return;
            }
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            if (!roles.isEmpty()) {
                context.getClaims().claim("roles", roles);
            }
        };
    }
}
