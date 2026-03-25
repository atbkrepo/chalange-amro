package com.xyz.auth.config;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
@Profile("dev")
@EnableConfigurationProperties(AppProperties.class)
@RequiredArgsConstructor
@Slf4j
public class OAuth2ClientInitializer {

    private final AppProperties props;

    @Bean
    ApplicationRunner registerClients(
            RegisteredClientRepository repository,
            PasswordEncoder encoder,
            JdbcUserDetailsManager userDetailsManager) {
        return args -> {
            registerServiceClient(repository, encoder);
            registerExternalAppClient(repository, encoder);
            registerAdminUser(userDetailsManager, encoder);
            registerAppUsers(userDetailsManager, encoder);
        };
    }

    private void registerServiceClient(RegisteredClientRepository repository, PasswordEncoder encoder) {
        var creds = props.getClients().getService();
        if (repository.findByClientId(creds.getClientId()) != null) {
            log.info("Service-to-service client '{}' already exists, skipping", creds.getClientId());
            return;
        }

        RegisteredClient serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(creds.getClientId())
                .clientSecret(encoder.encode(creds.getClientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("service.read")
                .scope("service.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .build())
                .build();

        repository.save(serviceClient);
        log.info("Registered service-to-service client '{}'", creds.getClientId());
    }

    private void registerExternalAppClient(RegisteredClientRepository repository, PasswordEncoder encoder) {
        var creds = props.getClients().getExternal();
        if (repository.findByClientId(creds.getClientId()) != null) {
            log.info("External app client '{}' already exists, skipping", creds.getClientId());
            return;
        }

        RegisteredClient externalClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(creds.getClientId())
                .clientSecret(encoder.encode(creds.getClientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://localhost:8080/login/oauth2/code/auth-server")
                .redirectUri("http://localhost:8080/authorized")
                .redirectUri("http://localhost:8080/authorized.html")
                .redirectUri("http://localhost:8080/swagger-ui/oauth2-redirect.html")
                .postLogoutRedirectUri("http://localhost:8080/logged-out")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        repository.save(externalClient);
        log.info("Registered external app client '{}'", creds.getClientId());
    }

    private void registerAdminUser(JdbcUserDetailsManager userDetailsManager, PasswordEncoder encoder) {
        var admin = props.getAdmin();
        createUser(userDetailsManager, encoder, admin.getUsername(), admin.getPassword(), admin.getRoles().toArray(String[]::new));
    }

    private void registerAppUsers(JdbcUserDetailsManager userDetailsManager, PasswordEncoder encoder) {
        props.getUsers().forEach(entry ->
                createUser(userDetailsManager, encoder, entry.getUsername(), entry.getPassword(), entry.getRoles().toArray(String[]::new)));
    }

    private static void createUser(JdbcUserDetailsManager mgr, PasswordEncoder encoder,
                                   String name, String password, String... roles) {
        if (mgr.userExists(name)) {
            log.info("User '{}' already exists, skipping", name);
            return;
        }

        UserDetails user = User.builder()
                .username(name)
                .password(encoder.encode(password))
                .roles(roles)
                .build();

        mgr.createUser(user);
        log.info("Created user '{}' with roles {}", name, roles);
    }
}
