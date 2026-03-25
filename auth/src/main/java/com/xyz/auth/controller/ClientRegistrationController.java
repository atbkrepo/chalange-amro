package com.xyz.auth.controller;

import com.xyz.auth.dto.ClientRegistrationRequest;
import com.xyz.auth.dto.ClientRegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Client Registration", description = "OAuth2 client registration management")
@SecurityRequirement(name = "basicAuth")
public class ClientRegistrationController {

    private static final long DEFAULT_ACCESS_TOKEN_TTL = 3600L;
    private static final long DEFAULT_REFRESH_TOKEN_TTL = 604800L;

    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientRegistrationController(RegisteredClientRepository clientRepository,
                                        PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    @Operation(summary = "Register a new OAuth2 client")
    public ResponseEntity<ClientRegistrationResponse> register(@RequestBody ClientRegistrationRequest request) {
        if (clientRepository.findByClientId(request.clientId()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Client '" + request.clientId() + "' already exists");
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.clientId())
                .clientSecret(passwordEncoder.encode(request.clientSecret()));

        if (request.authenticationMethods() != null) {
            request.authenticationMethods()
                    .forEach(m -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(m)));
        }

        if (request.grantTypes() != null) {
            request.grantTypes()
                    .forEach(g -> builder.authorizationGrantType(new AuthorizationGrantType(g)));
        }

        if (request.redirectUris() != null) {
            request.redirectUris().forEach(builder::redirectUri);
        }

        if (request.postLogoutRedirectUris() != null) {
            request.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        }

        if (request.scopes() != null) {
            request.scopes().forEach(builder::scope);
        }

        builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(request.requireAuthorizationConsent())
                .build());

        long accessTtl = request.accessTokenTtlSeconds() != null
                ? request.accessTokenTtlSeconds() : DEFAULT_ACCESS_TOKEN_TTL;
        long refreshTtl = request.refreshTokenTtlSeconds() != null
                ? request.refreshTokenTtlSeconds() : DEFAULT_REFRESH_TOKEN_TTL;
        boolean reuseRefresh = request.reuseRefreshTokens() != null
                && request.reuseRefreshTokens();

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(accessTtl))
                .refreshTokenTimeToLive(Duration.ofSeconds(refreshTtl))
                .reuseRefreshTokens(reuseRefresh)
                .build());

        RegisteredClient client = builder.build();
        clientRepository.save(client);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(client));
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Get a registered OAuth2 client by client ID")
    public ClientRegistrationResponse getClient(@PathVariable String clientId) {
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Client '" + clientId + "' not found");
        }
        return toResponse(client);
    }

    private ClientRegistrationResponse toResponse(RegisteredClient client) {
        return new ClientRegistrationResponse(
                client.getId(),
                client.getClientId(),
                client.getClientName(),
                toStringSet(client.getClientAuthenticationMethods()),
                toGrantTypeStrings(client.getAuthorizationGrantTypes()),
                client.getRedirectUris(),
                client.getPostLogoutRedirectUris(),
                client.getScopes()
        );
    }

    private Set<String> toStringSet(Set<ClientAuthenticationMethod> methods) {
        if (methods == null) return Collections.emptySet();
        return methods.stream()
                .map(ClientAuthenticationMethod::getValue)
                .collect(Collectors.toSet());
    }

    private Set<String> toGrantTypeStrings(Set<AuthorizationGrantType> grantTypes) {
        if (grantTypes == null) return Collections.emptySet();
        return grantTypes.stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.toSet());
    }
}
