package com.subscriptiontracker.service;

import com.subscriptiontracker.model.entity.Subscription;

public interface EmailService {

    /**
     * Sends an email reminder for an upcoming subscription renewal.
     *
     * @param subscription the subscription renewing soon
     * @param daysBefore number of days before renewal
     * @return true if the email was sent successfully, false otherwise
     */
    boolean sendRenewalReminder(Subscription subscription, int daysBefore);

    /**
     * Sends an email reminder for an expiring free trial.
     *
     * @param subscription the trial subscription expiring soon
     * @param daysBefore number of days before expiry
     * @return true if the email was sent successfully, false otherwise
     */
    boolean sendTrialExpiryReminder(Subscription subscription, int daysBefore);
}
