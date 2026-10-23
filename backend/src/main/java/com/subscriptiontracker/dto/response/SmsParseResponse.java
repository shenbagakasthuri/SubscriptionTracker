package com.subscriptiontracker.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.TransactionDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsParseResponse {

    private BigDecimal amount;
    private String currency;
    private String merchant;
    private LocalDate transactionDate;
    private TransactionDirection direction;
    private PaymentMethod paymentMethod;
    private String maskedAccount;
    private String confidence;

    @JsonProperty("isDebit")
    private boolean isDebit;

    @JsonProperty("isSupported")
    private boolean isSupported;

    private String parseMessage;
}
