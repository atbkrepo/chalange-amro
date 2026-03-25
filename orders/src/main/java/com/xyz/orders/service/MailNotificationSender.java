package com.xyz.orders.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailNotificationSender {

    private final JavaMailSender mailSender;

    @CircuitBreaker(name = "emailSend", fallbackMethod = "sendFallback")
    public void send(SimpleMailMessage message) throws MailException {
        this.mailSender.send(message);
    }

    @SuppressWarnings("unused")
    private void sendFallback(SimpleMailMessage message, Throwable t) {
        log.error("Email send skipped (circuit open or failure): {}", t.toString());
    }
}
