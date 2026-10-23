package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.SmsParseRequest;
import com.subscriptiontracker.dto.response.SmsParseResponse;
import com.subscriptiontracker.dto.response.SmsTransactionResponse;
import com.subscriptiontracker.dto.response.SubscriptionSuggestionResponse;
import com.subscriptiontracker.exception.DuplicateResourceException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.mapper.SmsTransactionMapper;
import com.subscriptiontracker.model.entity.SmsTransaction;
import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.model.enums.*;
import com.subscriptiontracker.repository.SmsTransactionRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.service.impl.SmsTransactionServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsTransactionServiceImplTest {

    @Mock
    private SmsParserService smsParserService;

    @Mock
    private SmsTransactionRepository smsTransactionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private SmsTransactionMapper smsTransactionMapper = new SmsTransactionMapper();

    @InjectMocks
    private SmsTransactionServiceImpl smsTransactionService;

    private User mockUser;
    private SmsTransaction mockTransaction;
    private SmsParseResponse validParseResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .name("Arun Kumar")
                .email("arun@example.com")
                .password("encoded_pass")
                .build();

        validParseResponse = SmsParseResponse.builder()
                .amount(new BigDecimal("499.00"))
                .currency("INR")
                .merchant("NETFLIX")
                .transactionDate(LocalDate.of(2026, 10, 9))
                .direction(TransactionDirection.DEBIT)
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .maskedAccount("XX1234")
                .confidence("HIGH")
                .isDebit(true)
                .isSupported(true)
                .build();

        mockTransaction = SmsTransaction.builder()
                .id(10L)
                .user(mockUser)
                .merchant("NETFLIX")
                .amount(new BigDecimal("499.00"))
                .currency("INR")
                .transactionDate(LocalDate.of(2026, 10, 9))
                .direction(TransactionDirection.DEBIT)
                .paymentMethod(PaymentMethod.UPI_AUTOPAY)
                .maskedAccount("XX1234")
                .deduplicationFingerprint("test-fingerprint")
                .importedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully import a valid debit transaction")
    void importTransaction_Success() {
        SmsParseRequest request = new SmsParseRequest("Rs.499.00 debited from your account on 09-10-2026 at NETFLIX.");

        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsParserService.parseSms(request.getSmsText())).thenReturn(validParseResponse);
        when(smsTransactionRepository.existsByUserIdAndDeduplicationFingerprint(eq(1L), anyString())).thenReturn(false);
        when(smsTransactionRepository.save(any(SmsTransaction.class))).thenReturn(mockTransaction);

        SmsTransactionResponse response = smsTransactionService.importTransaction(request, "arun@example.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("NETFLIX", response.getMerchant());
        assertEquals(new BigDecimal("499.00"), response.getAmount());
        assertEquals(TransactionDirection.DEBIT, response.getDirection());

        verify(smsTransactionRepository, times(1)).save(any(SmsTransaction.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate transaction import")
    void importTransaction_Duplicate_ThrowsDuplicateResourceException() {
        SmsParseRequest request = new SmsParseRequest("Rs.499.00 debited from your account on 09-10-2026 at NETFLIX.");

        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsParserService.parseSms(request.getSmsText())).thenReturn(validParseResponse);
        when(smsTransactionRepository.existsByUserIdAndDeduplicationFingerprint(eq(1L), anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> smsTransactionService.importTransaction(request, "arun@example.com"));

        verify(smsTransactionRepository, never()).save(any(SmsTransaction.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when importing unsupported message")
    void importTransaction_UnsupportedSms_ThrowsIllegalArgumentException() {
        SmsParseRequest request = new SmsParseRequest("Your OTP is 123456.");
        SmsParseResponse unsupportedResponse = SmsParseResponse.builder()
                .isSupported(false)
                .isDebit(false)
                .parseMessage("OTP rejected")
                .build();

        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsParserService.parseSms(request.getSmsText())).thenReturn(unsupportedResponse);

        assertThrows(IllegalArgumentException.class,
                () -> smsTransactionService.importTransaction(request, "arun@example.com"));

        verify(smsTransactionRepository, never()).save(any(SmsTransaction.class));
    }

    @Test
    @DisplayName("Should retrieve only authenticated user's transactions")
    void getUserTransactions_Success() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsTransactionRepository.findByUserIdOrderByTransactionDateDesc(1L)).thenReturn(List.of(mockTransaction));

        List<SmsTransactionResponse> transactions = smsTransactionService.getUserTransactions("arun@example.com");

        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals(1L, transactions.get(0).getUserId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when accessing another user's transaction")
    void getTransactionById_BelongsToAnotherUser_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsTransactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> smsTransactionService.getTransactionById(999L, "arun@example.com"));
    }

    @Test
    @DisplayName("Should identify recurring monthly pattern from multiple transactions")
    void getSubscriptionSuggestions_DetectsMonthlyPattern() {
        SmsTransaction t1 = SmsTransaction.builder()
                .id(1L)
                .user(mockUser)
                .merchant("NETFLIX")
                .amount(new BigDecimal("499.00"))
                .transactionDate(LocalDate.of(2026, 8, 9))
                .direction(TransactionDirection.DEBIT)
                .build();

        SmsTransaction t2 = SmsTransaction.builder()
                .id(2L)
                .user(mockUser)
                .merchant("NETFLIX")
                .amount(new BigDecimal("499.00"))
                .transactionDate(LocalDate.of(2026, 9, 9))
                .direction(TransactionDirection.DEBIT)
                .build();

        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsTransactionRepository.findByUserIdAndDirectionOrderByTransactionDateAsc(1L, TransactionDirection.DEBIT))
                .thenReturn(List.of(t1, t2));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(List.of());

        List<SubscriptionSuggestionResponse> suggestions = smsTransactionService.getSubscriptionSuggestions("arun@example.com");

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        SubscriptionSuggestionResponse suggestion = suggestions.get(0);
        assertEquals("NETFLIX", suggestion.getMerchantName());
        assertEquals(BillingCycle.MONTHLY, suggestion.getSuggestedBillingCycle());
        assertEquals(Category.OTT, suggestion.getSuggestedCategory());
        assertEquals(new BigDecimal("499.00"), suggestion.getSuggestedAmount());
        assertFalse(suggestion.isAlreadySubscribed());
        assertEquals(2, suggestion.getOccurrenceCount());
    }

    @Test
    @DisplayName("Should return empty suggestions when fewer than 2 transactions exist")
    void getSubscriptionSuggestions_SingleTransaction_ReturnsEmpty() {
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsTransactionRepository.findByUserIdAndDirectionOrderByTransactionDateAsc(1L, TransactionDirection.DEBIT))
                .thenReturn(List.of(mockTransaction));

        List<SubscriptionSuggestionResponse> suggestions = smsTransactionService.getSubscriptionSuggestions("arun@example.com");

        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    @DisplayName("Should flag alreadySubscribed as true when an active matching subscription exists")
    void getSubscriptionSuggestions_AlreadySubscribedFlag() {
        SmsTransaction t1 = SmsTransaction.builder()
                .id(1L)
                .user(mockUser)
                .merchant("SPOTIFY")
                .amount(new BigDecimal("119.00"))
                .transactionDate(LocalDate.of(2026, 8, 1))
                .direction(TransactionDirection.DEBIT)
                .build();

        SmsTransaction t2 = SmsTransaction.builder()
                .id(2L)
                .user(mockUser)
                .merchant("SPOTIFY")
                .amount(new BigDecimal("119.00"))
                .transactionDate(LocalDate.of(2026, 9, 1))
                .direction(TransactionDirection.DEBIT)
                .build();

        Subscription activeSub = Subscription.builder()
                .id(50L)
                .user(mockUser)
                .name("Spotify Premium")
                .status(SubscriptionStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(smsTransactionRepository.findByUserIdAndDirectionOrderByTransactionDateAsc(1L, TransactionDirection.DEBIT))
                .thenReturn(List.of(t1, t2));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(List.of(activeSub));

        List<SubscriptionSuggestionResponse> suggestions = smsTransactionService.getSubscriptionSuggestions("arun@example.com");

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertTrue(suggestions.get(0).isAlreadySubscribed());
    }
}
