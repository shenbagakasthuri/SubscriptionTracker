package com.subscriptiontracker.service;

import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.model.enums.BillingCycle;
import com.subscriptiontracker.model.enums.Category;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import com.subscriptiontracker.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    private User user;
    private Subscription activeSubscription;
    private Subscription trialSubscription;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "notifications@subscriptiontracker.com");

        user = User.builder()
                .id(1L)
                .name("Alex Hunter")
                .email("alex@example.com")
                .build();

        activeSubscription = Subscription.builder()
                .id(10L)
                .user(user)
                .name("Netflix Premium")
                .category(Category.OTT)
                .amount(new BigDecimal("649.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(3))
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .cancelUrl("https://netflix.com/cancel")
                .build();

        trialSubscription = Subscription.builder()
                .id(20L)
                .user(user)
                .name("Spotify Premium Free Trial")
                .category(Category.MUSIC)
                .amount(new BigDecimal("119.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(1))
                .paymentMethod(PaymentMethod.CARD)
                .isTrial(true)
                .trialEndDate(LocalDate.now().plusDays(1))
                .status(SubscriptionStatus.ACTIVE)
                .cancelUrl("https://spotify.com/cancel")
                .build();
    }

    private MimeMessage createMockMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    @DisplayName("Should successfully send renewal reminder email to subscription owner")
    void sendRenewalReminder_Success() throws Exception {
        MimeMessage mimeMessage = createMockMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/renewal-alert"), any(Context.class))).thenReturn("<html>Renewal Alert</html>");

        boolean sent = emailService.sendRenewalReminder(activeSubscription, 3);

        assertTrue(sent);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertEquals("Upcoming Renewal Alert: Netflix Premium", mimeMessage.getSubject());
        assertEquals("alex@example.com", mimeMessage.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("Should return false and not throw exception when email delivery fails")
    void sendRenewalReminder_DeliveryFailure_ReturnsFalse() {
        MimeMessage mimeMessage = createMockMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/renewal-alert"), any(Context.class))).thenReturn("<html>Renewal Alert</html>");
        doThrow(new MailSendException("SMTP server connection timeout")).when(mailSender).send(any(MimeMessage.class));

        boolean sent = emailService.sendRenewalReminder(activeSubscription, 3);

        assertFalse(sent);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should return false when user email is missing or empty")
    void sendRenewalReminder_MissingEmail_ReturnsFalse() {
        User userWithoutEmail = User.builder().id(2L).name("No Email").email("   ").build();
        activeSubscription.setUser(userWithoutEmail);

        boolean sent = emailService.sendRenewalReminder(activeSubscription, 3);

        assertFalse(sent);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should successfully send trial expiry reminder email")
    void sendTrialExpiryReminder_Success() throws Exception {
        MimeMessage mimeMessage = createMockMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/trial-ending-alert"), any(Context.class))).thenReturn("<html>Trial Ending Alert</html>");

        boolean sent = emailService.sendTrialExpiryReminder(trialSubscription, 1);

        assertTrue(sent);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertEquals("Free Trial Ending Soon: Spotify Premium Free Trial", mimeMessage.getSubject());
        assertEquals("alex@example.com", mimeMessage.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("Should return false when trial expiry email delivery fails")
    void sendTrialExpiryReminder_DeliveryFailure_ReturnsFalse() {
        MimeMessage mimeMessage = createMockMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/trial-ending-alert"), any(Context.class))).thenReturn("<html>Trial Ending Alert</html>");
        doThrow(new MailSendException("Authentication rejected")).when(mailSender).send(any(MimeMessage.class));

        boolean sent = emailService.sendTrialExpiryReminder(trialSubscription, 1);

        assertFalse(sent);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should return false when subscription is null")
    void sendRenewalReminder_NullSubscription_ReturnsFalse() {
        boolean sent = emailService.sendRenewalReminder(null, 3);
        assertFalse(sent);
        verifyNoInteractions(mailSender);
    }
}
