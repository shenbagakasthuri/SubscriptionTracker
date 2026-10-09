package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.request.SubscriptionRequest;
import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.model.enums.BillingCycle;
import com.subscriptiontracker.model.enums.Category;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;
    private Subscription user1Subscription;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .name("User One")
                .email("user1@example.com")
                .password(passwordEncoder.encode("Password123"))
                .build());

        user2 = userRepository.save(User.builder()
                .name("User Two")
                .email("user2@example.com")
                .password(passwordEncoder.encode("Password123"))
                .build());

        user1Token = "Bearer " + jwtTokenProvider.generateToken(user1.getEmail(), user1.getId());
        user2Token = "Bearer " + jwtTokenProvider.generateToken(user2.getEmail(), user2.getId());

        user1Subscription = subscriptionRepository.save(Subscription.builder()
                .user(user1)
                .name("Spotify Premium")
                .category(Category.MUSIC)
                .amount(new BigDecimal("119.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(15))
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("POST /api/subscriptions should return 201 Created for authenticated user")
    void createSubscription_Authenticated_Returns201() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .name("Hotstar Super")
                .category(Category.OTT)
                .amount(new BigDecimal("899.00"))
                .billingCycle(BillingCycle.YEARLY)
                .nextRenewalDate(LocalDate.now().plusYears(1))
                .paymentMethod(PaymentMethod.CARD)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("created successfully")))
                .andExpect(jsonPath("$.data.name", is("Hotstar Super")))
                .andExpect(jsonPath("$.data.category", is("OTT")))
                .andExpect(jsonPath("$.data.amount", is(899.00)))
                .andExpect(jsonPath("$.data.userId", is(user1.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/subscriptions should return 401 for unauthenticated request")
    void createSubscription_Unauthenticated_Returns401() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .name("Hotstar Super")
                .category(Category.OTT)
                .amount(new BigDecimal("899.00"))
                .billingCycle(BillingCycle.YEARLY)
                .nextRenewalDate(LocalDate.now().plusYears(1))
                .paymentMethod(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/subscriptions should reject missing required name with 400 Bad Request")
    void createSubscription_MissingName_Returns400() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .category(Category.OTT)
                .amount(new BigDecimal("899.00"))
                .billingCycle(BillingCycle.YEARLY)
                .nextRenewalDate(LocalDate.now().plusYears(1))
                .paymentMethod(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.data.name", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/subscriptions should reject zero or negative amounts with 400 Bad Request")
    void createSubscription_ZeroOrNegativeAmount_Returns400() throws Exception {
        SubscriptionRequest zeroAmountRequest = SubscriptionRequest.builder()
                .name("Invalid Zero Sub")
                .category(Category.SOFTWARE)
                .amount(BigDecimal.ZERO)
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(30))
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroAmountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.data.amount", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/subscriptions should reject trial=true without trialEndDate with 400 Bad Request")
    void createSubscription_TrialWithoutEndDate_Returns400() throws Exception {
        SubscriptionRequest trialRequest = SubscriptionRequest.builder()
                .name("Trial Subscription")
                .category(Category.SOFTWARE)
                .amount(new BigDecimal("199.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(30))
                .paymentMethod(PaymentMethod.CARD)
                .isTrial(true)
                .trialEndDate(null)
                .build();

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trialRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/subscriptions should return only owned subscriptions")
    void getUserSubscriptions_ReturnsOnlyOwnedSubscriptions() throws Exception {
        mockMvc.perform(get("/api/subscriptions")
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Spotify Premium")));

        mockMvc.perform(get("/api/subscriptions")
                        .header("Authorization", user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/subscriptions/{id} should return 200 for owned subscription")
    void getSubscriptionById_Owned_Returns200() throws Exception {
        mockMvc.perform(get("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(user1Subscription.getId().intValue())))
                .andExpect(jsonPath("$.data.name", is("Spotify Premium")));
    }

    @Test
    @DisplayName("GET /api/subscriptions/{id} should return 404 when requested by another user")
    void getSubscriptionById_OwnedByAnotherUser_Returns404() throws Exception {
        mockMvc.perform(get("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Subscription not found")));
    }

    @Test
    @DisplayName("PUT /api/subscriptions/{id} should update subscription successfully for owner")
    void updateSubscription_Owned_Returns200() throws Exception {
        SubscriptionRequest updateRequest = SubscriptionRequest.builder()
                .name("Spotify Family")
                .category(Category.MUSIC)
                .amount(new BigDecimal("199.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(20))
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .isTrial(false)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Spotify Family")))
                .andExpect(jsonPath("$.data.amount", is(199.00)));
    }

    @Test
    @DisplayName("PUT /api/subscriptions/{id} should return 404 when user tries to update another user's subscription")
    void updateSubscription_OwnedByAnotherUser_Returns404() throws Exception {
        SubscriptionRequest updateRequest = SubscriptionRequest.builder()
                .name("Hacked Name")
                .category(Category.MUSIC)
                .amount(new BigDecimal("1.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .nextRenewalDate(LocalDate.now().plusDays(1))
                .paymentMethod(PaymentMethod.CARD)
                .build();

        mockMvc.perform(put("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/subscriptions/{id} should return 204 No Content for owner")
    void deleteSubscription_Owned_Returns204() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user1Token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/subscriptions/{id} should return 404 when user tries to delete another user's subscription")
    void deleteSubscription_OwnedByAnotherUser_Returns404() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));

        // Verify user1's subscription was not deleted
        mockMvc.perform(get("/api/subscriptions/{id}", user1Subscription.getId())
                        .header("Authorization", user1Token))
                .andExpect(status().isOk());
    }
}
