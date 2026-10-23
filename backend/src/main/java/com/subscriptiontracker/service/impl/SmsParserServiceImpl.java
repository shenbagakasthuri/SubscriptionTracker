package com.subscriptiontracker.service.impl;

import com.subscriptiontracker.dto.response.SmsParseResponse;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.TransactionDirection;
import com.subscriptiontracker.service.SmsParserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SmsParserServiceImpl implements SmsParserService {

    // 1. Rejected Message Types (OTPs, Verification)
    private static final Pattern OTP_PATTERN = Pattern.compile(
            "(?i)\\b(otp|one\\s+time\\s+password|verification\\s+code|secret\\s+code|security\\s+code|do\\s+not\\s+share|valid\\s+for\\s+\\d+\\s+min)\\b"
    );

    // 2. Direction Patterns
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
            "(?i)\\b(credited\\s+to|credited\\s+with|has\\s+been\\s+credited|received\\s+from|deposited|added\\s+to\\s+your\\s+account|salary\\s+credited)\\b"
    );

    private static final Pattern REFUND_PATTERN = Pattern.compile(
            "(?i)\\b(refund|refunded|reversal|reversed|cashback\\s+received)\\b"
    );

    private static final Pattern DEBIT_PATTERN = Pattern.compile(
            "(?i)\\b(debited|spent|spent\\s+on|payment\\s+of|paid\\s+to|purchase\\s+of|charged|transferred|autopay|mandate\\s+debited)\\b"
    );

    // 3. Amount Patterns
    private static final List<Pattern> AMOUNT_PATTERNS = List.of(
            Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"),
            Pattern.compile("(?i)(?:payment\\s+of|spent|debited\\s+for|amount\\s+of)\\s+(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)")
    );

    // 4. Merchant Patterns
    private static final List<Pattern> MERCHANT_PATTERNS = List.of(
            // "at NETFLIX on", "at SPOTIFY on", "to merchant@example was", "for Netflix", "at PHASE4 TEST SERVICE."
            Pattern.compile("(?i)(?:\\bat|\\bto|\\bfor|\\bvpa)\\s+([A-Za-z0-9@._& -]+?)(?=[.,;]?\\s+(?:on|using|via|ref|bal|avl|avail|thru|was\\s+successful|successful|mandate|#)\\b|[.,;]+(?:\\s+|$)|\\s*$)"),
            // fallback: "at <name>" or "to <name>" or "for <name>"
            Pattern.compile("(?i)(?:\\bat|\\bto|\\bfor|\\bvpa)\\s+([A-Za-z0-9@._&-]+)")
    );

    // Stop words that cannot be merchant names
    private static final Set<String> STOP_WORDS = Set.of(
            "your", "account", "a/c", "acct", "card", "bank", "vpa", "upi", "merchant", "my", "our", "the"
    );

    // 5. Date Formats Supported
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMMM yyyy").toFormatter(Locale.ENGLISH)
    );

    private static final Pattern DATE_REGEX = Pattern.compile(
            "\\b(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}(?:\\s+|-)[A-Za-z]{3,9}(?:\\s+|-)\\d{2,4})\\b"
    );

    // 6. Payment Method Patterns
    private static final Pattern UPI_PATTERN = Pattern.compile("(?i)\\b(upi|vpa|autopay|mandate)\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("(?i)\\b(card|debit\\s+card|credit\\s+card|visa|mastercard|rupay)\\b");
    private static final Pattern NETBANKING_PATTERN = Pattern.compile("(?i)\\b(netbanking|net\\s+banking|internet\\s+banking)\\b");

    // 7. Masked Account Pattern
    private static final Pattern MASKED_ACCOUNT_PATTERN = Pattern.compile(
            "(?i)(?:a/c|account|card|acct)\\s*(?:no\\.?)?\\s*(?:ending\\s+with|ending|#)?\\s*(?:[xX*]+)?([0-9]{3,4})"
    );

    @Override
    public SmsParseResponse parseSms(String smsText) {
        if (smsText == null || smsText.trim().isEmpty()) {
            return SmsParseResponse.builder()
                    .isSupported(false)
                    .isDebit(false)
                    .direction(TransactionDirection.UNKNOWN)
                    .confidence("LOW")
                    .parseMessage("SMS text cannot be empty")
                    .build();
        }

        String cleanedText = smsText.trim();

        // 1. Detect and reject OTPs or security codes
        if (OTP_PATTERN.matcher(cleanedText).find()) {
            return SmsParseResponse.builder()
                    .isSupported(false)
                    .isDebit(false)
                    .direction(TransactionDirection.UNKNOWN)
                    .confidence("LOW")
                    .parseMessage("Unsupported message: OTP and authentication alerts are not debit transactions")
                    .build();
        }

        // 2. Detect and reject Credit transactions
        if (CREDIT_PATTERN.matcher(cleanedText).find() && !DEBIT_PATTERN.matcher(cleanedText).find()) {
            return SmsParseResponse.builder()
                    .isSupported(false)
                    .isDebit(false)
                    .direction(TransactionDirection.CREDIT)
                    .confidence("LOW")
                    .parseMessage("Unsupported message: Credit transactions cannot be imported as subscriptions")
                    .build();
        }

        // 3. Detect and reject Refunds
        if (REFUND_PATTERN.matcher(cleanedText).find() && !DEBIT_PATTERN.matcher(cleanedText).find()) {
            return SmsParseResponse.builder()
                    .isSupported(false)
                    .isDebit(false)
                    .direction(TransactionDirection.REFUND)
                    .confidence("LOW")
                    .parseMessage("Unsupported message: Refund transactions cannot be imported as subscriptions")
                    .build();
        }

        // 4. Check for Debit Intent
        boolean hasDebitKeyword = DEBIT_PATTERN.matcher(cleanedText).find();
        if (!hasDebitKeyword) {
            return SmsParseResponse.builder()
                    .isSupported(false)
                    .isDebit(false)
                    .direction(TransactionDirection.UNKNOWN)
                    .confidence("LOW")
                    .parseMessage("Unsupported message: Could not reliably determine debit transaction")
                    .build();
        }

        // 5. Extract Amount
        BigDecimal amount = extractAmount(cleanedText);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return SmsParseResponse.builder()
                    .isSupported(false)
                    .isDebit(true)
                    .direction(TransactionDirection.DEBIT)
                    .confidence("LOW")
                    .parseMessage("Unable to reliably determine transaction amount")
                    .build();
        }

        // 6. Extract Merchant
        String merchant = extractMerchant(cleanedText);
        if (merchant == null || merchant.isBlank()) {
            return SmsParseResponse.builder()
                    .amount(amount)
                    .currency("INR")
                    .isSupported(false)
                    .isDebit(true)
                    .direction(TransactionDirection.DEBIT)
                    .confidence("LOW")
                    .parseMessage("Unable to reliably determine merchant or payee name")
                    .build();
        }

        // 7. Extract Transaction Date
        LocalDate transactionDate = extractDate(cleanedText);
        if (transactionDate == null) {
            return SmsParseResponse.builder()
                    .amount(amount)
                    .merchant(merchant)
                    .currency("INR")
                    .isSupported(false)
                    .isDebit(true)
                    .direction(TransactionDirection.DEBIT)
                    .confidence("LOW")
                    .parseMessage("Unable to reliably determine transaction date")
                    .build();
        }

        // 8. Extract Payment Method (optional)
        PaymentMethod paymentMethod = extractPaymentMethod(cleanedText);

        // 9. Extract Masked Account (optional)
        String maskedAccount = extractMaskedAccount(cleanedText);

        // All essential debit fields found
        boolean hasAllRequiredFields = amount != null && amount.compareTo(BigDecimal.ZERO) > 0
                && merchant != null && !merchant.isBlank()
                && transactionDate != null;

        return SmsParseResponse.builder()
                .amount(amount)
                .currency("INR")
                .merchant(merchant)
                .transactionDate(transactionDate)
                .direction(TransactionDirection.DEBIT)
                .paymentMethod(paymentMethod)
                .maskedAccount(maskedAccount)
                .confidence(hasAllRequiredFields ? "HIGH" : "LOW")
                .isDebit(true)
                .isSupported(hasAllRequiredFields)
                .parseMessage("Parsed debit transaction successfully")
                .build();
    }

    private BigDecimal extractAmount(String text) {
        for (Pattern pattern : AMOUNT_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String rawAmount = matcher.group(1).replace(",", "").trim();
                try {
                    return new BigDecimal(rawAmount);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private String extractMerchant(String text) {
        for (Pattern pattern : MERCHANT_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String candidate = matcher.group(1).trim();
                // Clean punctuation from candidate ends
                candidate = candidate.replaceAll("^[\\s,.;:!?\\-]+|[\\s,.;:!?\\-]+$", "");
                candidate = candidate.replaceAll("\\s+", " ").trim();
                if (!candidate.isBlank() && !isStopWordCandidate(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isStopWordCandidate(String candidate) {
        String lower = candidate.toLowerCase();
        if (STOP_WORDS.contains(lower)) {
            return true;
        }
        String[] words = lower.split("\\s+");
        return words.length > 0 && Arrays.stream(words).allMatch(STOP_WORDS::contains);
    }

    private LocalDate extractDate(String text) {
        Matcher matcher = DATE_REGEX.matcher(text);
        while (matcher.find()) {
            String dateStr = matcher.group(1).trim();
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return null;
    }

    private PaymentMethod extractPaymentMethod(String text) {
        if (UPI_PATTERN.matcher(text).find()) {
            return PaymentMethod.UPI_AUTOPAY;
        } else if (CARD_PATTERN.matcher(text).find()) {
            return PaymentMethod.CARD;
        } else if (NETBANKING_PATTERN.matcher(text).find()) {
            return PaymentMethod.NETBANKING;
        }
        return PaymentMethod.OTHER;
    }

    private String extractMaskedAccount(String text) {
        Matcher matcher = MASKED_ACCOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            String digits = matcher.group(1);
            return "XX" + digits;
        }
        return null;
    }
}
