package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.response.SmsParseResponse;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.TransactionDirection;
import com.subscriptiontracker.service.impl.SmsParserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SmsParserServiceImplTest {

    private SmsParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new SmsParserServiceImpl();
    }

    @Test
    @DisplayName("Should successfully parse debit SMS with Rs. prefix")
    void parseSms_StandardDebitWithRs() {
        String sms = "Rs.499.00 debited from your account on 09-10-2026 at NETFLIX.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertTrue(response.isSupported());
        assertTrue(response.isDebit());
        assertEquals(new BigDecimal("499.00"), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals("NETFLIX", response.getMerchant());
        assertEquals(LocalDate.of(2026, 10, 9), response.getTransactionDate());
        assertEquals(TransactionDirection.DEBIT, response.getDirection());
        assertEquals("HIGH", response.getConfidence());
    }

    @Test
    @DisplayName("Should successfully parse card debit with INR and text month")
    void parseSms_CardDebitWithTextMonth() {
        String sms = "INR 199.00 spent on your card at SPOTIFY on 09 Oct 2026.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertTrue(response.isSupported());
        assertTrue(response.isDebit());
        assertEquals(new BigDecimal("199.00"), response.getAmount());
        assertEquals("SPOTIFY", response.getMerchant());
        assertEquals(LocalDate.of(2026, 10, 9), response.getTransactionDate());
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
    }

    @Test
    @DisplayName("Should parse UPI payment with merchant email and slash date")
    void parseSms_UpiPaymentWithEmailMerchant() {
        String sms = "Your UPI payment of Rs 299 to merchant@example was successful on 09/10/2026.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertTrue(response.isSupported());
        assertTrue(response.isDebit());
        assertEquals(new BigDecimal("299"), response.getAmount());
        assertEquals("merchant@example", response.getMerchant());
        assertEquals(LocalDate.of(2026, 10, 9), response.getTransactionDate());
        assertEquals(PaymentMethod.UPI_AUTOPAY, response.getPaymentMethod());
    }

    @Test
    @DisplayName("Should parse currency symbol ₹ and comma formatted amounts")
    void parseSms_RupeeSymbolAndCommaAmount() {
        String sms = "₹1,299.50 debited for DISNEY_HOTSTAR on 2026-10-09 via UPI AutoPay.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertTrue(response.isSupported());
        assertEquals(new BigDecimal("1299.50"), response.getAmount());
        assertEquals("DISNEY_HOTSTAR", response.getMerchant());
        assertEquals(LocalDate.of(2026, 10, 9), response.getTransactionDate());
        assertEquals(PaymentMethod.UPI_AUTOPAY, response.getPaymentMethod());
    }

    @Test
    @DisplayName("Should reject OTP and authentication messages")
    void parseSms_RejectOtpMessage() {
        String sms = "Your OTP for transaction of Rs.499 at NETFLIX is 492810. Do not share with anyone.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertFalse(response.isSupported());
        assertFalse(response.isDebit());
        assertTrue(response.getParseMessage().contains("OTP"));
    }

    @Test
    @DisplayName("Should reject credit transactions")
    void parseSms_RejectCreditMessage() {
        String sms = "Rs.50000.00 credited to your account ending with 1234 on 01-10-2026.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertFalse(response.isSupported());
        assertFalse(response.isDebit());
        assertEquals(TransactionDirection.CREDIT, response.getDirection());
        assertTrue(response.getParseMessage().contains("Credit transactions cannot be imported"));
    }

    @Test
    @DisplayName("Should reject refund messages")
    void parseSms_RejectRefundMessage() {
        String sms = "Refund of Rs.199.00 initiated from SPOTIFY on 09-10-2026.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertFalse(response.isSupported());
        assertFalse(response.isDebit());
        assertEquals(TransactionDirection.REFUND, response.getDirection());
        assertTrue(response.getParseMessage().contains("Refund transactions cannot be imported"));
    }

    @Test
    @DisplayName("Should successfully parse multi-word merchant at end of message with trailing punctuation")
    void parseSms_MultiWordMerchantAtEndOfMessageWithTrailingPunctuation() {
        String sms = "Rs.327.00 debited from your account on 23-10-2026 at PHASE4 TEST SERVICE.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertTrue(response.isSupported());
        assertTrue(response.isDebit());
        assertEquals(new BigDecimal("327.00"), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals("PHASE4 TEST SERVICE", response.getMerchant());
        assertEquals(LocalDate.of(2026, 10, 23), response.getTransactionDate());
        assertEquals(TransactionDirection.DEBIT, response.getDirection());
        assertEquals("HIGH", response.getConfidence());
    }

    @Test
    @DisplayName("Should parse multi-word merchant at end of message without punctuation")
    void parseSms_MultiWordMerchantAtEndOfMessageWithoutPunctuation() {
        String sms = "Rs.327.00 debited from your account on 23-10-2026 at PHASE4 TEST SERVICE";
        SmsParseResponse response = parserService.parseSms(sms);

        assertTrue(response.isSupported());
        assertEquals("PHASE4 TEST SERVICE", response.getMerchant());
        assertEquals(new BigDecimal("327.00"), response.getAmount());
        assertEquals(LocalDate.of(2026, 10, 23), response.getTransactionDate());
        assertEquals("HIGH", response.getConfidence());
    }

    @Test
    @DisplayName("Should parse multi-word merchant in middle of message followed by date")
    void parseSms_MultiWordMerchantInMiddleOfMessage() {
        String sms = "Rs.327.00 debited from your account at PHASE4 TEST SERVICE on 23-10-2026.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertTrue(response.isSupported());
        assertEquals("PHASE4 TEST SERVICE", response.getMerchant());
        assertEquals(new BigDecimal("327.00"), response.getAmount());
        assertEquals(LocalDate.of(2026, 10, 23), response.getTransactionDate());
        assertEquals("HIGH", response.getConfidence());
    }

    @Test
    @DisplayName("Should reject messages with missing amount and set confidence to LOW")
    void parseSms_RejectMissingAmount() {
        String sms = "Debited from your account at NETFLIX on 09-10-2026.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertFalse(response.isSupported());
        assertEquals("LOW", response.getConfidence());
        assertNotEquals("HIGH", response.getConfidence());
        assertTrue(response.getParseMessage().contains("amount"));
    }

    @Test
    @DisplayName("Should reject messages with missing merchant and set confidence to LOW")
    void parseSms_RejectMissingMerchant() {
        String sms = "Rs.499.00 debited from your account on 09-10-2026.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertFalse(response.isSupported());
        assertEquals("LOW", response.getConfidence());
        assertNotEquals("HIGH", response.getConfidence());
        assertTrue(response.getParseMessage().contains("merchant"));
    }

    @Test
    @DisplayName("Should reject messages with missing or unsupported date and set confidence to LOW")
    void parseSms_RejectMissingDate() {
        String sms = "Rs.499.00 debited from your account at NETFLIX.";
        SmsParseResponse response = parserService.parseSms(sms);

        assertFalse(response.isSupported());
        assertEquals("LOW", response.getConfidence());
        assertNotEquals("HIGH", response.getConfidence());
        assertTrue(response.getParseMessage().contains("date"));
    }

    @Test
    @DisplayName("Should safely handle empty or whitespace-only messages and set confidence to LOW")
    void parseSms_EmptyMessage() {
        SmsParseResponse response = parserService.parseSms("   ");

        assertFalse(response.isSupported());
        assertEquals("LOW", response.getConfidence());
        assertNotEquals("HIGH", response.getConfidence());
        assertTrue(response.getParseMessage().contains("empty"));
    }
}
