package com.xyz.auth.controller;

import java.util.Set;
import java.util.stream.Collectors;

import com.xyz.auth.dto.UserRegistrationRequest;
import com.xyz.auth.dto.UserRegistrationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User registration and management")
@SecurityRequirement(name = "basicAuth")
public class UserManagementController {

    private final JdbcUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(JdbcUserDetailsManager userDetailsManager,
                                    PasswordEncoder passwordEncoder) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserRegistrationResponse> createUser(@RequestBody UserRegistrationRequest request) {
        if (userDetailsManager.userExists(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User '" + request.username() + "' already exists");
        }

        boolean enabled = request.enabled() == null || request.enabled();
        Set<String> roles = request.roles() != null ? request.roles() : Set.of("USER");

        UserDetails user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .disabled(!enabled)
                .roles(roles.toArray(String[]::new))
                .build();

        userDetailsManager.createUser(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get user details by username")
    public UserRegistrationResponse getUser(@PathVariable String username) {
        if (!userDetailsManager.userExists(username)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User '" + username + "' not found");
        }
        return toResponse(userDetailsManager.loadUserByUsername(username));
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        if (!userDetailsManager.userExists(username)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User '" + username + "' not found");
        }
        userDetailsManager.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    private UserRegistrationResponse toResponse(UserDetails user) {
        Set<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return new UserRegistrationResponse(
                user.getUsername(),
                user.isEnabled(),
                authorities
        );
    }
}
