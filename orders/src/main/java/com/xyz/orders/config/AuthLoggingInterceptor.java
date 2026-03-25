package com.xyz.orders.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.stream.Collectors;

@Component
@Slf4j
public class AuthLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.joining(", "));
        log.info("{} {} — principal={}, roles=[{}]", request.getMethod(), request.getRequestURI(), auth.getName(), roles);
        return true;
    }
}
