package com.subscriptiontracker.service;

import com.subscriptiontracker.model.entity.NotificationLog;
import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.model.enums.BillingCycle;
import com.subscriptiontracker.model.enums.Category;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import com.subscriptiontracker.repository.NotificationLogRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User user;
    private Subscription activeSubscription;
    private Subscription cancelledSubscription;
    private Subscription activeTrialSubscription;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "renewalDaysBefore", 3);
        ReflectionTestUtils.setField(notificationService, "trialDaysBefore", 1);

        user = User.builder()
                .id(1L)
                .name("Sarah Connor")
                .email("sarah@example.com")
                .build();

        activeSubscription = Subscription.builder()
                .id(100L)
                .user(user)
                .name("AWS Cloud")
                .category(Category.SOFTWARE)
                .amount(new BigDecimal("1200.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(3))
                .paymentMethod(PaymentMethod.CARD)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        cancelledSubscription = Subscription.builder()
                .id(101L)
                .user(user)
                .name("Cancelled Gym")
                .category(Category.FITNESS)
                .amount(new BigDecimal("1500.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(3))
                .paymentMethod(PaymentMethod.OTHER)
                .isTrial(false)
                .status(SubscriptionStatus.CANCELLED)
                .build();

        activeTrialSubscription = Subscription.builder()
                .id(200L)
                .user(user)
                .name("YouTube Premium Trial")
                .category(Category.OTT)
                .amount(new BigDecimal("129.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(1))
                .trialEndDate(LocalDate.now().plusDays(1))
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .isTrial(true)
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should successfully send renewal reminder and persist NotificationLog when email delivers")
    void sendRenewalReminders_Success() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        when(subscriptionRepository.findByStatusAndNextRenewalDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(activeSubscription));
        when(notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                eq(100L), eq("RENEWAL"), any(LocalDateTime.class))).thenReturn(false);
        when(emailService.sendRenewalReminder(activeSubscription, 3)).thenReturn(true);

        int sent = notificationService.sendRenewalReminders();

        assertEquals(1, sent);
        verify(emailService, times(1)).sendRenewalReminder(activeSubscription, 3);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(logCaptor.capture());
        NotificationLog savedLog = logCaptor.getValue();
        assertEquals("RENEWAL", savedLog.getAlertType());
        assertEquals(activeSubscription, savedLog.getSubscription());
    }

    @Test
    @DisplayName("Should never notify cancelled subscriptions")
    void sendRenewalReminders_NeverNotifiesCancelledSubscriptions() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        // Even if repository returned a cancelled subscription defensively
        when(subscriptionRepository.findByStatusAndNextRenewalDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(cancelledSubscription));

        int sent = notificationService.sendRenewalReminders();

        assertEquals(0, sent);
        verify(emailService, never()).sendRenewalReminder(any(), anyInt());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip sending when reminder was already sent today (duplicate prevention)")
    void sendRenewalReminders_DuplicatePrevention() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        when(subscriptionRepository.findByStatusAndNextRenewalDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(activeSubscription));
        when(notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                eq(100L), eq("RENEWAL"), any(LocalDateTime.class))).thenReturn(true);

        int sent = notificationService.sendRenewalReminders();

        assertEquals(0, sent);
        verify(emailService, never()).sendRenewalReminder(any(), anyInt());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should never claim email was sent and never save NotificationLog when delivery fails")
    void sendRenewalReminders_DeliveryFailure_DoesNotPersistLog() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        when(subscriptionRepository.findByStatusAndNextRenewalDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(activeSubscription));
        when(notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                eq(100L), eq("RENEWAL"), any(LocalDateTime.class))).thenReturn(false);
        when(emailService.sendRenewalReminder(activeSubscription, 3)).thenReturn(false);

        int sent = notificationService.sendRenewalReminders();

        assertEquals(0, sent);
        verify(emailService, times(1)).sendRenewalReminder(activeSubscription, 3);
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip subscription if owner email is missing")
    void sendRenewalReminders_MissingOwnerEmail() {
        activeSubscription.setUser(User.builder().id(2L).name("No Email").email("").build());
        LocalDate targetDate = LocalDate.now().plusDays(3);
        when(subscriptionRepository.findByStatusAndNextRenewalDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(activeSubscription));

        int sent = notificationService.sendRenewalReminders();

        assertEquals(0, sent);
        verifyNoInteractions(emailService);
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully send trial expiry reminder and persist NotificationLog")
    void sendTrialExpiryReminders_Success() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        when(subscriptionRepository.findByStatusAndIsTrialTrueAndTrialEndDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(activeTrialSubscription));
        when(notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                eq(200L), eq("TRIAL_EXPIRY"), any(LocalDateTime.class))).thenReturn(false);
        when(emailService.sendTrialExpiryReminder(activeTrialSubscription, 1)).thenReturn(true);

        int sent = notificationService.sendTrialExpiryReminders();

        assertEquals(1, sent);
        verify(emailService, times(1)).sendTrialExpiryReminder(activeTrialSubscription, 1);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(logCaptor.capture());
        assertEquals("TRIAL_EXPIRY", logCaptor.getValue().getAlertType());
    }

    @Test
    @DisplayName("Should prevent duplicate trial expiry reminder sent on the same day")
    void sendTrialExpiryReminders_DuplicatePrevention() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        when(subscriptionRepository.findByStatusAndIsTrialTrueAndTrialEndDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(activeTrialSubscription));
        when(notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                eq(200L), eq("TRIAL_EXPIRY"), any(LocalDateTime.class))).thenReturn(true);

        int sent = notificationService.sendTrialExpiryReminders();

        assertEquals(0, sent);
        verify(emailService, never()).sendTrialExpiryReminder(any(), anyInt());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not save log when trial expiry email delivery fails")
    void sendTrialExpiryReminders_DeliveryFailure_DoesNotPersistLog() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        when(subscriptionRepository.findByStatusAndIsTrialTrueAndTrialEndDate(SubscriptionStatus.ACTIVE, targetDate))
                .thenReturn(List.of(activeTrialSubscription));
        when(notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                eq(200L), eq("TRIAL_EXPIRY"), any(LocalDateTime.class))).thenReturn(false);
        when(emailService.sendTrialExpiryReminder(activeTrialSubscription, 1)).thenReturn(false);

        int sent = notificationService.sendTrialExpiryReminders();

        assertEquals(0, sent);
        verify(emailService, times(1)).sendTrialExpiryReminder(activeTrialSubscription, 1);
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should aggregate both renewal and trial reminder counts in processDailyReminders")
    void processDailyReminders_AggregatesCounts() {
        LocalDate renewalDate = LocalDate.now().plusDays(3);
        LocalDate trialDate = LocalDate.now().plusDays(1);

        when(subscriptionRepository.findByStatusAndNextRenewalDate(SubscriptionStatus.ACTIVE, renewalDate))
                .thenReturn(List.of(activeSubscription));
        when(subscriptionRepository.findByStatusAndIsTrialTrueAndTrialEndDate(SubscriptionStatus.ACTIVE, trialDate))
                .thenReturn(List.of(activeTrialSubscription));

        when(notificationLogRepository.existsBySubscriptionIdAndAlertTypeAndSentAtAfter(
                anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);

        when(emailService.sendRenewalReminder(activeSubscription, 3)).thenReturn(true);
        when(emailService.sendTrialExpiryReminder(activeTrialSubscription, 1)).thenReturn(true);

        int totalSent = notificationService.processDailyReminders();

        assertEquals(2, totalSent);
        verify(emailService, times(1)).sendRenewalReminder(activeSubscription, 3);
        verify(emailService, times(1)).sendTrialExpiryReminder(activeTrialSubscription, 1);
        verify(notificationLogRepository, times(2)).save(any(NotificationLog.class));
    }
}
