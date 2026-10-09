package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.SubscriptionRequest;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.mapper.SubscriptionMapper;
import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.model.enums.BillingCycle;
import com.subscriptiontracker.model.enums.Category;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private SubscriptionMapper subscriptionMapper = new SubscriptionMapper();

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User mockUser;
    private Subscription mockSubscription;
    private SubscriptionRequest validRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .name("Arun Kumar")
                .email("arun@example.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .build();

        validRequest = SubscriptionRequest.builder()
                .name("Netflix")
                .category(Category.OTT)
                .amount(new BigDecimal("649.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusMonths(1))
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .cancelUrl("https://netflix.com/cancel")
                .build();

        mockSubscription = Subscription.builder()
                .id(100L)
                .user(mockUser)
                .name("Netflix")
                .category(Category.OTT)
                .amount(new BigDecimal("649.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusMonths(1))
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .cancelUrl("https://netflix.com/cancel")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create subscription successfully for authenticated user")
    void createSubscription_Success() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(mockSubscription);

        SubscriptionResponse response = subscriptionService.createSubscription(validRequest, "arun@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Netflix", response.getName());
        assertEquals(Category.OTT, response.getCategory());
        assertEquals(new BigDecimal("649.00"), response.getAmount());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());

        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when isTrial is true but trialEndDate is null")
    void createSubscription_InvalidTrialDates_ThrowsIllegalArgumentException() {
        validRequest.setIsTrial(true);
        validRequest.setTrialEndDate(null);
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> subscriptionService.createSubscription(validRequest, "arun@example.com"));

        assertEquals("Trial end date is required when isTrial is true", ex.getMessage());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should return only subscriptions owned by the user")
    void getUserSubscriptions_Success() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(List.of(mockSubscription));

        List<SubscriptionResponse> results = subscriptionService.getUserSubscriptions("arun@example.com");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Netflix", results.get(0).getName());
        assertEquals(1L, results.get(0).getUserId());
    }

    @Test
    @DisplayName("Should return empty list when user has no subscriptions")
    void getUserSubscriptions_EmptyList() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        List<SubscriptionResponse> results = subscriptionService.getUserSubscriptions("arun@example.com");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should retrieve subscription by ID when owned by user")
    void getSubscriptionById_Success() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockSubscription));

        SubscriptionResponse response = subscriptionService.getSubscriptionById(100L, "arun@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Netflix", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when subscription belongs to another user")
    void getSubscriptionById_OwnedByAnotherUser_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.getSubscriptionById(999L, "arun@example.com"));
    }

    @Test
    @DisplayName("Should update subscription successfully for owning user")
    void updateSubscription_Success() {
        SubscriptionRequest updateRequest = SubscriptionRequest.builder()
                .name("Netflix Premium UHD")
                .category(Category.OTT)
                .amount(new BigDecimal("799.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusMonths(1))
                .paymentMethod(PaymentMethod.CARD)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(mockSubscription);

        SubscriptionResponse response = subscriptionService.updateSubscription(100L, updateRequest, "arun@example.com");

        assertNotNull(response);
        assertEquals("Netflix Premium UHD", mockSubscription.getName());
        assertEquals(new BigDecimal("799.00"), mockSubscription.getAmount());
        assertEquals(PaymentMethod.CARD, mockSubscription.getPaymentMethod());
        assertEquals(1L, mockSubscription.getUser().getId()); // owner unchanged
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating subscription of another user")
    void updateSubscription_OwnedByAnotherUser_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.updateSubscription(999L, validRequest, "arun@example.com"));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should delete subscription successfully for owning user")
    void deleteSubscription_Success() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(mockSubscription));

        subscriptionService.deleteSubscription(100L, "arun@example.com");

        verify(subscriptionRepository, times(1)).delete(mockSubscription);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting subscription of another user")
    void deleteSubscription_OwnedByAnotherUser_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.deleteSubscription(999L, "arun@example.com"));
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }
}
