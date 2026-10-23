package com.subscriptiontracker.service.impl;

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
import com.subscriptiontracker.service.SmsParserService;
import com.subscriptiontracker.service.SmsTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmsTransactionServiceImpl implements SmsTransactionService {

    private final SmsParserService smsParserService;
    private final SmsTransactionRepository smsTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SmsTransactionMapper smsTransactionMapper;

    @Override
    @Transactional
    public SmsTransactionResponse importTransaction(SmsParseRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        // 1. Server-side parsing (never trust client-supplied parsed fields)
        SmsParseResponse parsed = smsParserService.parseSms(request.getSmsText());
        if (!parsed.isSupported() || !parsed.isDebit()) {
            throw new IllegalArgumentException("Cannot import unsupported message: " + parsed.getParseMessage());
        }

        // 2. Generate deduplication fingerprint (scoped to this user)
        String fingerprint = generateDeduplicationFingerprint(
                user.getId(),
                parsed.getMerchant(),
                parsed.getAmount(),
                parsed.getTransactionDate(),
                parsed.getDirection()
        );

        // 3. Reject duplicate import
        if (smsTransactionRepository.existsByUserIdAndDeduplicationFingerprint(user.getId(), fingerprint)) {
            throw new DuplicateResourceException("Transaction has already been imported for this user");
        }

        // 4. Persist transaction (raw SMS text is discarded for privacy)
        SmsTransaction transaction = SmsTransaction.builder()
                .user(user)
                .merchant(parsed.getMerchant())
                .amount(parsed.getAmount())
                .currency(parsed.getCurrency())
                .transactionDate(parsed.getTransactionDate())
                .direction(parsed.getDirection())
                .paymentMethod(parsed.getPaymentMethod())
                .maskedAccount(parsed.getMaskedAccount())
                .deduplicationFingerprint(fingerprint)
                .build();

        SmsTransaction saved = smsTransactionRepository.save(transaction);
        return smsTransactionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmsTransactionResponse> getUserTransactions(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<SmsTransaction> transactions = smsTransactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
        return smsTransactionMapper.toResponseList(transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public SmsTransactionResponse getTransactionById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        SmsTransaction transaction = smsTransactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        return smsTransactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionSuggestionResponse> getSubscriptionSuggestions(String userEmail) {
        User user = getUserByEmail(userEmail);

        List<SmsTransaction> debitTransactions = smsTransactionRepository
                .findByUserIdAndDirectionOrderByTransactionDateAsc(user.getId(), TransactionDirection.DEBIT);

        if (debitTransactions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Subscription> userSubscriptions = subscriptionRepository.findByUserId(user.getId());

        return analyzeRecurringTransactions(debitTransactions, userSubscriptions);
    }

    private List<SubscriptionSuggestionResponse> analyzeRecurringTransactions(
            List<SmsTransaction> transactions,
            List<Subscription> userSubscriptions) {

        // Group by normalized merchant
        Map<String, List<SmsTransaction>> merchantGroups = transactions.stream()
                .collect(Collectors.groupingBy(t -> normalizeMerchant(t.getMerchant())));

        List<SubscriptionSuggestionResponse> suggestions = new ArrayList<>();

        for (Map.Entry<String, List<SmsTransaction>> entry : merchantGroups.entrySet()) {
            List<SmsTransaction> group = new ArrayList<>(entry.getValue());
            if (group.size() < 2) {
                continue; // Requires at least 2 historical occurrences
            }

            group.sort(Comparator.comparing(SmsTransaction::getTransactionDate));

            BigDecimal firstAmount = group.get(0).getAmount();
            boolean consistentAmount = group.stream()
                    .allMatch(t -> t.getAmount().compareTo(firstAmount) == 0);

            List<Long> intervals = new ArrayList<>();
            for (int i = 1; i < group.size(); i++) {
                long days = ChronoUnit.DAYS.between(group.get(i - 1).getTransactionDate(), group.get(i).getTransactionDate());
                intervals.add(days);
            }

            BillingCycle cycle = determineBillingCycle(intervals);
            if (cycle != null) {
                SmsTransaction latest = group.get(group.size() - 1);
                LocalDate nextDate = calculateNextRenewalDate(latest.getTransactionDate(), cycle);
                Category category = inferCategory(latest.getMerchant());
                boolean alreadySubscribed = isAlreadySubscribed(latest.getMerchant(), userSubscriptions);

                suggestions.add(SubscriptionSuggestionResponse.builder()
                        .merchantName(latest.getMerchant())
                        .suggestedCategory(category)
                        .suggestedAmount(latest.getAmount())
                        .suggestedBillingCycle(cycle)
                        .suggestedPaymentMethod(latest.getPaymentMethod() != null ? latest.getPaymentMethod() : PaymentMethod.UPI_AUTOPAY)
                        .occurrenceCount(group.size())
                        .confidence(consistentAmount ? "HIGH" : "MEDIUM")
                        .lastTransactionDate(latest.getTransactionDate())
                        .nextPredictedRenewalDate(nextDate)
                        .transactionIds(group.stream().map(SmsTransaction::getId).toList())
                        .alreadySubscribed(alreadySubscribed)
                        .explanation("Observed recurring " + cycle.name().toLowerCase() + " charges (" + group.size() + " times) of ₹" + latest.getAmount() + " at " + latest.getMerchant())
                        .build());
            }
        }

        return suggestions;
    }

    private BillingCycle determineBillingCycle(List<Long> intervals) {
        if (intervals.isEmpty()) {
            return null;
        }

        double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);

        if (avgInterval >= 25.0 && avgInterval <= 35.0) {
            return BillingCycle.MONTHLY;
        } else if (avgInterval >= 80.0 && avgInterval <= 100.0) {
            return BillingCycle.QUARTERLY;
        } else if (avgInterval >= 350.0 && avgInterval <= 380.0) {
            return BillingCycle.YEARLY;
        }

        return null;
    }

    private LocalDate calculateNextRenewalDate(LocalDate lastDate, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> lastDate.plusMonths(1);
            case QUARTERLY -> lastDate.plusMonths(3);
            case YEARLY -> lastDate.plusYears(1);
        };
    }

    private Category inferCategory(String merchant) {
        String normalized = merchant.toLowerCase();
        if (normalized.contains("netflix") || normalized.contains("hotstar") || normalized.contains("prime")
                || normalized.contains("disney") || normalized.contains("sony") || normalized.contains("zee5")) {
            return Category.OTT;
        } else if (normalized.contains("spotify") || normalized.contains("wynk") || normalized.contains("gaana")
                || normalized.contains("music") || normalized.contains("apple")) {
            return Category.MUSIC;
        } else if (normalized.contains("jio") || normalized.contains("airtel") || normalized.contains("vi")
                || normalized.contains("bsnl")) {
            return Category.TELECOM;
        } else if (normalized.contains("github") || normalized.contains("chatgpt") || normalized.contains("openai")
                || normalized.contains("aws") || normalized.contains("google") || normalized.contains("microsoft")) {
            return Category.SOFTWARE;
        } else if (normalized.contains("gym") || normalized.contains("cult") || normalized.contains("fitness")) {
            return Category.FITNESS;
        } else if (normalized.contains("electricity") || normalized.contains("bescom") || normalized.contains("water")
                || normalized.contains("gas") || normalized.contains("broadband")) {
            return Category.UTILITIES;
        }
        return Category.OTHER;
    }

    private boolean isAlreadySubscribed(String merchant, List<Subscription> subscriptions) {
        String normalized = normalizeMerchant(merchant);
        return subscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .anyMatch(s -> {
                    String subName = normalizeMerchant(s.getName());
                    return subName.contains(normalized) || normalized.contains(subName);
                });
    }

    private String normalizeMerchant(String merchant) {
        if (merchant == null) return "";
        return merchant.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private User getUserByEmail(String userEmail) {
        String normalizedEmail = userEmail.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));
    }

    private String generateDeduplicationFingerprint(
            Long userId,
            String merchant,
            BigDecimal amount,
            LocalDate date,
            TransactionDirection direction) {

        String normalized = userId + ":" +
                normalizeMerchant(merchant) + ":" +
                amount.stripTrailingZeros().toPlainString() + ":" +
                date.toString() + ":" +
                direction.name();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
