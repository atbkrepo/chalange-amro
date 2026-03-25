package com.xyz.orders.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.notification.from-email:}")
    private String fromEmail;

    @Value("${app.notification.enabled:false}")
    private boolean notificationEnabled;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Async
    public void sendOrderConfirmation(String toEmail, String customerName,
                                      Long orderId, BigDecimal totalAmount) {
        if (!notificationEnabled) {
            log.info("Notifications disabled — Order #{} placed for {} (amount: {})", orderId, customerName, totalAmount);
            return;
        }

        if (StringUtils.isBlank(toEmail)) {
            log.warn("No email provided for order #{}, skipping notification", orderId);
            return;
        }

        if (StringUtils.isBlank(mailUsername) || StringUtils.isBlank(mailPassword)) {
            log.error("spring.mail.username / spring.mail.password are not set (use MAIL_USERNAME / MAIL_PASSWORD); skipping email for order #{}", orderId);
            return;
        }

        String from = StringUtils.isNotBlank(fromEmail) ? fromEmail : mailUsername;
        if (StringUtils.isBlank(from)) {
            log.error("No from-address configured (app.notification.from-email or MAIL_USERNAME); skipping email for order #{}", orderId);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Order Confirmation — Order #" + orderId);
            message.setText(String.format("""
                            Hello %s,
                            
                            Thank you for your order!
                            
                            Order ID: %d
                            Amount to be paid: $%s
                            
                            We will notify you once your order is shipped.
                            
                            Best regards,
                            Orders Team""",
                    customerName, orderId, totalAmount.toPlainString()));

            this.mailSender.send(message);
            log.info("Order confirmation email sent to {} for order #{}", toEmail, orderId);
        } catch (MailException ex) {
            log.error("Failed to send order confirmation email for order #{}: {}", orderId, ex.getMessage());
        }
    }
}
