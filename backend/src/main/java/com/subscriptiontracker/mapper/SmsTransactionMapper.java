package com.subscriptiontracker.mapper;

import com.subscriptiontracker.dto.response.SmsTransactionResponse;
import com.subscriptiontracker.model.entity.SmsTransaction;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SmsTransactionMapper {

    public SmsTransactionResponse toResponse(SmsTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        return SmsTransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUser() != null ? transaction.getUser().getId() : null)
                .merchant(transaction.getMerchant())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionDate(transaction.getTransactionDate())
                .direction(transaction.getDirection())
                .paymentMethod(transaction.getPaymentMethod())
                .maskedAccount(transaction.getMaskedAccount())
                .importedAt(transaction.getImportedAt())
                .build();
    }

    public List<SmsTransactionResponse> toResponseList(List<SmsTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        return transactions.stream()
                .map(this::toResponse)
                .toList();
    }
}
