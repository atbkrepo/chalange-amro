package com.xyz.auth.config;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

@Configuration
public class OAuth2ClientInitializer {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientInitializer.class);

    @Value("${app.clients.service.client-id}")
    private String serviceClientId;

    @Value("${app.clients.service.client-secret}")
    private String serviceClientSecret;

    @Value("${app.clients.external.client-id}")
    private String externalClientId;

    @Value("${app.clients.external.client-secret}")
    private String externalClientSecret;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    ApplicationRunner registerClients(
            RegisteredClientRepository repository,
            PasswordEncoder encoder,
            JdbcUserDetailsManager userDetailsManager) {
        return args -> {
            registerServiceClient(repository, encoder);
            registerExternalAppClient(repository, encoder);
            registerAdminUser(userDetailsManager, encoder);
        };
    }

    private void registerServiceClient(RegisteredClientRepository repository, PasswordEncoder encoder) {
        if (repository.findByClientId(serviceClientId) != null) {
            log.info("Service-to-service client '{}' already exists, skipping", serviceClientId);
            return;
        }

        RegisteredClient serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(serviceClientId)
                .clientSecret(encoder.encode(serviceClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("service.read")
                .scope("service.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .build())
                .build();

        repository.save(serviceClient);
        log.info("Registered service-to-service client '{}'", serviceClientId);
    }

    private void registerExternalAppClient(RegisteredClientRepository repository, PasswordEncoder encoder) {
        if (repository.findByClientId(externalClientId) != null) {
            log.info("External app client '{}' already exists, skipping", externalClientId);
            return;
        }

        RegisteredClient externalClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(externalClientId)
                .clientSecret(encoder.encode(externalClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://localhost:8080/login/oauth2/code/auth-server")
                .redirectUri("http://localhost:8080/authorized")
                .postLogoutRedirectUri("http://localhost:8080/logged-out")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        repository.save(externalClient);
        log.info("Registered external app client '{}'", externalClientId);
    }

    private void registerAdminUser(JdbcUserDetailsManager userDetailsManager, PasswordEncoder encoder) {
        if (userDetailsManager.userExists(adminUsername)) {
            log.info("Admin user '{}' already exists, skipping", adminUsername);
            return;
        }

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN", "USER")
                .build();

        userDetailsManager.createUser(admin);
        log.info("Created default admin user (username: {})", adminUsername);
    }
}
