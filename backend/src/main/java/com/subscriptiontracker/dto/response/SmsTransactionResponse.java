package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.TransactionDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTransactionResponse {

    private Long id;
    private Long userId;
    private String merchant;
    private BigDecimal amount;
    private String currency;
    private LocalDate transactionDate;
    private TransactionDirection direction;
    private PaymentMethod paymentMethod;
    private String maskedAccount;
    private LocalDateTime importedAt;
}
