package com.xyz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Registered user details")
public record UserRegistrationResponse(

        @Schema(description = "Username")
        String username,

        @Schema(description = "Whether the account is enabled")
        boolean enabled,

        @Schema(description = "Granted authorities")
        Set<String> authorities
) {
}
