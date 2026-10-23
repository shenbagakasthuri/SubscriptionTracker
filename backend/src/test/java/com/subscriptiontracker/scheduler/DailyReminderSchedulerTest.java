package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReminderSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DailyReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
    }

    @Test
    @DisplayName("Should invoke processDailyReminders when scheduler is enabled")
    void runDailyReminders_WhenEnabled_InvokesService() {
        when(notificationService.processDailyReminders()).thenReturn(3);

        scheduler.runDailyReminders();

        verify(notificationService, times(1)).processDailyReminders();
    }

    @Test
    @DisplayName("Should not invoke processDailyReminders when scheduler is disabled")
    void runDailyReminders_WhenDisabled_SkipsExecution() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.runDailyReminders();

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should handle exception thrown by notificationService gracefully without rethrowing")
    void runDailyReminders_ServiceThrowsException_HandledGracefully() {
        when(notificationService.processDailyReminders()).thenThrow(new RuntimeException("Database error"));

        // Should not throw exception
        scheduler.runDailyReminders();

        verify(notificationService, times(1)).processDailyReminders();
    }
}
