package com.xyz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Registered OAuth2 client details")
public record ClientRegistrationResponse(

        @Schema(description = "Internal ID")
        String id,

        @Schema(description = "Client identifier")
        String clientId,

        @Schema(description = "Client display name")
        String clientName,

        @Schema(description = "Authentication methods")
        Set<String> authenticationMethods,

        @Schema(description = "Grant types")
        Set<String> grantTypes,

        @Schema(description = "Redirect URIs")
        Set<String> redirectUris,

        @Schema(description = "Post-logout redirect URIs")
        Set<String> postLogoutRedirectUris,

        @Schema(description = "Scopes")
        Set<String> scopes
) {
}
