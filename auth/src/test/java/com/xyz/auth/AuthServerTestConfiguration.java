package com.xyz.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

/**
 * {@link org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService} deserializes
 * stored token metadata with Jackson 3; {@code roles} and other claims as JSON arrays fail type resolution.
 * In-memory storage avoids JDBC round-trips for authorization records during tests.
 */
@TestConfiguration
public class AuthServerTestConfiguration {

    @Bean
    @Primary
    OAuth2AuthorizationService oAuth2AuthorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }
}
