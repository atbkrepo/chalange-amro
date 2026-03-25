package com.xyz.auth.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration request")
public record UserRegistrationRequest(

        @Schema(description = "Username", example = "john")
        String username,

        @Schema(description = "Password (stored BCrypt-encoded)", example = "password123")
        String password,

        @Schema(description = "Whether the user account is enabled", defaultValue = "true")
        Boolean enabled,

        @Schema(description = "Roles assigned to the user (without ROLE_ prefix)", example = "[\"USER\"]")
        Set<String> roles
) {
}
