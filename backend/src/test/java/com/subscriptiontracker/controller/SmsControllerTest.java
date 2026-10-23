package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.request.SmsParseRequest;
import com.subscriptiontracker.model.entity.SmsTransaction;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.TransactionDirection;
import com.subscriptiontracker.repository.SmsTransactionRepository;
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
class SmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmsTransactionRepository smsTransactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;
    private SmsTransaction user1Transaction;

    @BeforeEach
    void setUp() {
        smsTransactionRepository.deleteAll();

        user1 = userRepository.findByEmail("sms_user1@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Kavya Sharma")
                        .email("sms_user1@example.com")
                        .password(passwordEncoder.encode("Password123"))
                        .build()));

        user2 = userRepository.findByEmail("sms_user2@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Rohan Verma")
                        .email("sms_user2@example.com")
                        .password(passwordEncoder.encode("Password123"))
                        .build()));

        user1Token = "Bearer " + jwtTokenProvider.generateToken(user1.getEmail(), user1.getId());
        user2Token = "Bearer " + jwtTokenProvider.generateToken(user2.getEmail(), user2.getId());

        user1Transaction = smsTransactionRepository.save(SmsTransaction.builder()
                .user(user1)
                .merchant("SPOTIFY")
                .amount(new BigDecimal("119.00"))
                .currency("INR")
                .transactionDate(LocalDate.of(2026, 9, 10))
                .direction(TransactionDirection.DEBIT)
                .paymentMethod(PaymentMethod.CARD)
                .maskedAccount("XX5678")
                .deduplicationFingerprint("fingerprint-spot-1")
                .build());
    }

    @Test
    @DisplayName("POST /api/sms/parse should return preview without saving")
    void parseSms_PreviewOnly_Returns200() throws Exception {
        SmsParseRequest request = new SmsParseRequest("Rs.499.00 debited from your account on 09-10-2026 at NETFLIX.");

        long countBefore = smsTransactionRepository.count();

        mockMvc.perform(post("/api/sms/parse")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.merchant", is("NETFLIX")))
                .andExpect(jsonPath("$.data.amount", is(499.00)))
                .andExpect(jsonPath("$.data.isSupported", is(true)))
                .andExpect(jsonPath("$.data.isDebit", is(true)));

        // Verify no record was created in the database during preview
        long countAfter = smsTransactionRepository.count();
        org.junit.jupiter.api.Assertions.assertEquals(countBefore, countAfter);
    }

    @Test
    @DisplayName("POST /api/sms/import should save transaction and return 201 Created")
    void importTransaction_ValidDebit_Returns201() throws Exception {
        SmsParseRequest request = new SmsParseRequest("Rs.649.00 debited from your account on 09-10-2026 at NETFLIX via UPI.");

        mockMvc.perform(post("/api/sms/import")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.merchant", is("NETFLIX")))
                .andExpect(jsonPath("$.data.amount", is(649.00)))
                .andExpect(jsonPath("$.data.userId", is(user1.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/sms/import should reject duplicate transaction with 409 Conflict")
    void importTransaction_Duplicate_Returns409() throws Exception {
        SmsParseRequest request = new SmsParseRequest("Rs.649.00 debited from your account on 09-10-2026 at NETFLIX.");

        // First import succeeds
        mockMvc.perform(post("/api/sms/import")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate import returns 409 Conflict
        mockMvc.perform(post("/api/sms/import")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already been imported")));
    }

    @Test
    @DisplayName("POST /api/sms/import should reject unsupported OTP messages with 400 Bad Request")
    void importTransaction_UnsupportedOtp_Returns400() throws Exception {
        SmsParseRequest request = new SmsParseRequest("Your OTP is 987654. Valid for 10 min.");

        mockMvc.perform(post("/api/sms/import")
                        .header("Authorization", user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/sms/imports should return only transactions owned by the user")
    void getImportedTransactions_ReturnsOnlyOwnedTransactions() throws Exception {
        mockMvc.perform(get("/api/sms/imports")
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].merchant", is("SPOTIFY")));

        mockMvc.perform(get("/api/sms/imports")
                        .header("Authorization", user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/sms/imports/{id} should return 200 for owned transaction")
    void getTransactionById_Owned_Returns200() throws Exception {
        mockMvc.perform(get("/api/sms/imports/{id}", user1Transaction.getId())
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(user1Transaction.getId().intValue())))
                .andExpect(jsonPath("$.data.merchant", is("SPOTIFY")));
    }

    @Test
    @DisplayName("GET /api/sms/imports/{id} should return 404 when requested by another user")
    void getTransactionById_OwnedByAnotherUser_Returns404() throws Exception {
        mockMvc.perform(get("/api/sms/imports/{id}", user1Transaction.getId())
                        .header("Authorization", user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/sms/subscription-suggestions should detect recurring pattern for user")
    void getSubscriptionSuggestions_DetectsRecurrence() throws Exception {
        // Add a second Spotify transaction for user1 with 30-day difference
        smsTransactionRepository.save(SmsTransaction.builder()
                .user(user1)
                .merchant("SPOTIFY")
                .amount(new BigDecimal("119.00"))
                .currency("INR")
                .transactionDate(LocalDate.of(2026, 10, 10))
                .direction(TransactionDirection.DEBIT)
                .paymentMethod(PaymentMethod.CARD)
                .deduplicationFingerprint("fingerprint-spot-2")
                .build());

        mockMvc.perform(get("/api/sms/subscription-suggestions")
                        .header("Authorization", user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].merchantName", is("SPOTIFY")))
                .andExpect(jsonPath("$.data[0].suggestedBillingCycle", is("MONTHLY")))
                .andExpect(jsonPath("$.data[0].occurrenceCount", is(2)));

        // User 2 has no transactions, so they should see 0 suggestions
        mockMvc.perform(get("/api/sms/subscription-suggestions")
                        .header("Authorization", user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("All SMS endpoints should return 401 for unauthenticated requests")
    void smsEndpoints_Unauthenticated_Returns401() throws Exception {
        SmsParseRequest request = new SmsParseRequest("Rs.499.00 debited at NETFLIX on 09-10-2026.");

        mockMvc.perform(post("/api/sms/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/sms/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/sms/imports"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/sms/imports/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/sms/subscription-suggestions"))
                .andExpect(status().isUnauthorized());
    }
}
