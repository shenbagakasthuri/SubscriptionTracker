package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailyReminderScheduler {

    private final NotificationService notificationService;

    @Value("${app.notifications.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${app.notifications.cron:0 0 9 * * ?}")
    public void runDailyReminders() {
        if (!enabled) {
            log.info("Daily reminder scheduler is disabled by configuration");
            return;
        }

        log.info("Starting daily reminder scheduled job...");
        try {
            int sentCount = notificationService.processDailyReminders();
            log.info("Daily reminder scheduled job completed. Total sent: {}", sentCount);
        } catch (Exception e) {
            log.error("Error occurred while running daily reminder scheduled job: {}", e.getMessage(), e);
        }
    }
}
