package com.xyz.orders.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private MailNotificationSender mailNotificationSender;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void configureMail() {
        ReflectionTestUtils.setField(notificationService, "fromEmail", "from@test.com");
        ReflectionTestUtils.setField(notificationService, "notificationEnabled", true);
        ReflectionTestUtils.setField(notificationService, "mailUsername", "smtp-user");
        ReflectionTestUtils.setField(notificationService, "mailPassword", "smtp-secret");
    }

    @Test
    @DisplayName("sends mail when notifications enabled and SMTP configured")
    void sendsWhenConfigured() {
        notificationService.sendOrderConfirmation("buyer@test.com", "Ann", 42L, new BigDecimal("19.99"));

        verify(mailNotificationSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("does not send when notifications disabled")
    void skipsWhenDisabled() {
        ReflectionTestUtils.setField(notificationService, "notificationEnabled", false);

        notificationService.sendOrderConfirmation("buyer@test.com", "Ann", 42L, BigDecimal.ONE);

        verifyNoInteractions(mailNotificationSender);
    }

    @Test
    @DisplayName("does not send when recipient email is blank")
    void skipsBlankEmail() {
        notificationService.sendOrderConfirmation("  ", "Ann", 42L, BigDecimal.ONE);

        verify(mailNotificationSender, never()).send(any());
    }

    @Test
    @DisplayName("does not send when SMTP credentials missing")
    void skipsMissingSmtp() {
        ReflectionTestUtils.setField(notificationService, "mailUsername", "");
        ReflectionTestUtils.setField(notificationService, "mailPassword", "");

        notificationService.sendOrderConfirmation("buyer@test.com", "Ann", 42L, BigDecimal.ONE);

        verify(mailNotificationSender, never()).send(any());
    }
}
