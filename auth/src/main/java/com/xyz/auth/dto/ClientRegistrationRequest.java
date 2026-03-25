package com.xyz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "OAuth2 client registration request")
public record ClientRegistrationRequest(

        @Schema(description = "Unique client identifier", example = "orders-app")
        String clientId,

        @Schema(description = "Client secret (stored BCrypt-encoded)", example = "orders-secret")
        String clientSecret,

        @Schema(description = "Authentication methods", example = "[\"client_secret_basic\", \"client_secret_post\"]")
        Set<String> authenticationMethods,

        @Schema(description = "Allowed grant types", example = "[\"authorization_code\", \"refresh_token\", \"client_credentials\"]")
        Set<String> grantTypes,

        @Schema(description = "Redirect URIs for authorization_code flow")
        Set<String> redirectUris,

        @Schema(description = "Post-logout redirect URIs")
        Set<String> postLogoutRedirectUris,

        @Schema(description = "OAuth2 scopes", example = "[\"openid\", \"profile\", \"orders.read\"]")
        Set<String> scopes,

        @Schema(description = "Require user consent screen", defaultValue = "true")
        boolean requireAuthorizationConsent,

        @Schema(description = "Access token TTL in seconds", example = "3600", defaultValue = "3600")
        Long accessTokenTtlSeconds,

        @Schema(description = "Refresh token TTL in seconds", example = "604800", defaultValue = "604800")
        Long refreshTokenTtlSeconds,

        @Schema(description = "Allow reuse of refresh tokens", defaultValue = "false")
        Boolean reuseRefreshTokens
) {
}
