package com.xyz.auth.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

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
