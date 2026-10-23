package com.subscriptiontracker.service.impl;

import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.notifications.from-email:notifications@subscriptiontracker.com}")
    private String fromEmail;

    @Override
    public boolean sendRenewalReminder(Subscription subscription, int daysBefore) {
        if (subscription == null) {
            log.warn("Cannot send renewal reminder: subscription is null");
            return false;
        }

        if (subscription.getUser() == null || subscription.getUser().getEmail() == null || subscription.getUser().getEmail().isBlank()) {
            log.warn("Cannot send renewal reminder: subscription ID {} has no valid user email", subscription.getId());
            return false;
        }

        try {
            Context context = new Context();
            context.setVariable("userName", subscription.getUser().getName() != null ? subscription.getUser().getName() : "User");
            context.setVariable("subscriptionName", subscription.getName());
            context.setVariable("amount", subscription.getAmount() != null ? subscription.getAmount().toPlainString() : "0.00");
            context.setVariable("renewalDate", subscription.getNextRenewalDate() != null ? subscription.getNextRenewalDate().toString() : "");
            context.setVariable("paymentMethod", subscription.getPaymentMethod() != null ? subscription.getPaymentMethod().name() : "N/A");
            context.setVariable("cancelUrl", subscription.getCancelUrl());

            String htmlBody = templateEngine.process("email/renewal-alert", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(subscription.getUser().getEmail());
            helper.setSubject("Upcoming Renewal Alert: " + subscription.getName());
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Renewal reminder sent successfully to {} for subscription '{}' (ID: {})",
                    subscription.getUser().getEmail(), subscription.getName(), subscription.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send renewal reminder email to {} for subscription '{}' (ID: {}): {}",
                    subscription.getUser() != null ? subscription.getUser().getEmail() : "unknown",
                    subscription.getName(), subscription.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendTrialExpiryReminder(Subscription subscription, int daysBefore) {
        if (subscription == null) {
            log.warn("Cannot send trial expiry reminder: subscription is null");
            return false;
        }

        if (subscription.getUser() == null || subscription.getUser().getEmail() == null || subscription.getUser().getEmail().isBlank()) {
            log.warn("Cannot send trial expiry reminder: subscription ID {} has no valid user email", subscription.getId());
            return false;
        }

        try {
            Context context = new Context();
            context.setVariable("userName", subscription.getUser().getName() != null ? subscription.getUser().getName() : "User");
            context.setVariable("subscriptionName", subscription.getName());
            context.setVariable("amount", subscription.getAmount() != null ? subscription.getAmount().toPlainString() : "0.00");
            context.setVariable("trialEndDate", subscription.getTrialEndDate() != null ? subscription.getTrialEndDate().toString() : "");
            context.setVariable("cancelUrl", subscription.getCancelUrl());

            String htmlBody = templateEngine.process("email/trial-ending-alert", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(subscription.getUser().getEmail());
            helper.setSubject("Free Trial Ending Soon: " + subscription.getName());
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Trial expiry reminder sent successfully to {} for subscription '{}' (ID: {})",
                    subscription.getUser().getEmail(), subscription.getName(), subscription.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send trial expiry reminder email to {} for subscription '{}' (ID: {}): {}",
                    subscription.getUser() != null ? subscription.getUser().getEmail() : "unknown",
                    subscription.getName(), subscription.getId(), e.getMessage());
            return false;
        }
    }
}
