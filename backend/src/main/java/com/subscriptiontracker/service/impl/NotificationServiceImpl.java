package com.subscriptiontracker.service.impl;

import com.subscriptiontracker.model.entity.NotificationLog;
import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import com.subscriptiontracker.repository.NotificationLogRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.service.EmailService;
import com.subscriptiontracker.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailService emailService;

    @Value("${app.notifications.renewal-days-before:3}")
    private int renewalDaysBefore;

    @Value("${app.notifications.trial-days-before:1}")
    private int trialDaysBefore;

    @Override
    @Transactional
    public int processDailyReminders() {
        log.info("Processing daily subscription reminders...");
        int renewals = sendRenewalReminders();
        int trials = sendTrialExpiryReminders();
        int total = renewals + trials;
        log.info("Daily reminders processed. Sent: {} renewal reminders, {} trial expiry reminders (Total: {})",
                renewals, trials, total);
        return total;
    }

    @Override
    @Transactional
    public int sendRenewalReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(renewalDaysBefore);
        log.info("Checking for active subscriptions renewing on {} (in {} days)", targetDate, renewalDaysBefore);

        List<Subscription> subscriptions = subscriptionRepository.findByStatusAndNextRenewalDate(
                SubscriptionStatus.ACTIVE, targetDate);

        LocalDateTime cutoffTime = LocalDate.now().atStartOfDay();
        int sentCount = 0;

        for (Subscription subscription : subscriptions) {
            // Never notify for cancelled subscriptions
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                continue;
            }

            // Send reminders only to the subscription owner's email
            if (subscription.getUser() == null || subscription.getUser().getEmail() == null || subscription.getUser().getEmail().isBlank()) {
                log.warn("Subscription ID {} has no owner email. Skipping.", subscription.getId());
                continue;
            }

            // Duplicate prevention: skip if already notified today
            boolean alreadyNotified = notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                    subscription.getId(), ALERT_TYPE_RENEWAL, cutoffTime);
            if (alreadyNotified) {
                log.debug("Renewal reminder already sent today for subscription ID {}. Skipping.", subscription.getId());
                continue;
            }

            // Never claim an email was sent unless delivery succeeded
            boolean success = emailService.sendRenewalReminder(subscription, renewalDaysBefore);
            if (success) {
                NotificationLog logEntry = NotificationLog.builder()
                        .subscription(subscription)
                        .alertType(ALERT_TYPE_RENEWAL)
                        .build();
                notificationLogRepository.save(logEntry);
                sentCount++;
            } else {
                log.warn("Skipping notification log creation for subscription ID {} because email delivery failed", subscription.getId());
            }
        }

        return sentCount;
    }

    @Override
    @Transactional
    public int sendTrialExpiryReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(trialDaysBefore);
        log.info("Checking for active free trials expiring on {} (in {} days)", targetDate, trialDaysBefore);

        List<Subscription> trialSubscriptions = subscriptionRepository.findByStatusAndIsTrialTrueAndTrialEndDate(
                SubscriptionStatus.ACTIVE, targetDate);

        LocalDateTime cutoffTime = LocalDate.now().atStartOfDay();
        int sentCount = 0;

        for (Subscription subscription : trialSubscriptions) {
            // Never notify for cancelled subscriptions
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                continue;
            }

            // Send reminders only to the subscription owner's email
            if (subscription.getUser() == null || subscription.getUser().getEmail() == null || subscription.getUser().getEmail().isBlank()) {
                log.warn("Trial subscription ID {} has no owner email. Skipping.", subscription.getId());
                continue;
            }

            // Duplicate prevention: skip if already notified today
            boolean alreadyNotified = notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                    subscription.getId(), ALERT_TYPE_TRIAL_EXPIRY, cutoffTime);
            if (alreadyNotified) {
                log.debug("Trial expiry reminder already sent today for subscription ID {}. Skipping.", subscription.getId());
                continue;
            }

            // Never claim an email was sent unless delivery succeeded
            boolean success = emailService.sendTrialExpiryReminder(subscription, trialDaysBefore);
            if (success) {
                NotificationLog logEntry = NotificationLog.builder()
                        .subscription(subscription)
                        .alertType(ALERT_TYPE_TRIAL_EXPIRY)
                        .build();
                notificationLogRepository.save(logEntry);
                sentCount++;
            } else {
                log.warn("Skipping notification log creation for trial subscription ID {} because email delivery failed", subscription.getId());
            }
        }

        return sentCount;
    }
}
