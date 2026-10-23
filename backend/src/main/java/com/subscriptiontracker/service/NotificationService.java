package com.subscriptiontracker.service;

public interface NotificationService {

    String ALERT_TYPE_RENEWAL = "RENEWAL";
    String ALERT_TYPE_TRIAL_EXPIRY = "TRIAL_EXPIRY";

    /**
     * Process all daily reminders (both upcoming renewals and expiring free trials).
     *
     * @return total count of reminders successfully sent
     */
    int processDailyReminders();

    /**
     * Send reminders for active subscriptions renewing soon.
     *
     * @return count of renewal reminders successfully sent
     */
    int sendRenewalReminders();

    /**
     * Send reminders for active free trials expiring soon.
     *
     * @return count of trial expiry reminders successfully sent
     */
    int sendTrialExpiryReminders();
}
