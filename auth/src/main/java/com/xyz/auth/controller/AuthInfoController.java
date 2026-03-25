package com.xyz.auth.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth", description = "Authentication and token information")
public class AuthInfoController {

    @GetMapping("/userinfo")
    @Operation(summary = "Get current user info from JWT token")
    @SecurityRequirement(name = "bearer")
    public Map<String, Object> userInfo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "sub", jwt.getSubject(),
                "claims", jwt.getClaims());
    }
}
