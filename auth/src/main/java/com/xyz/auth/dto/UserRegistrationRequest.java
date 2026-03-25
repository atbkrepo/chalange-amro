package com.xyz.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

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
