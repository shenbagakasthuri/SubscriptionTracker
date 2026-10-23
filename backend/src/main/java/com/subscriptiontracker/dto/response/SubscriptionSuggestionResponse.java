package com.subscriptiontracker.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subscriptiontracker.model.enums.BillingCycle;
import com.subscriptiontracker.model.enums.Category;
import com.subscriptiontracker.model.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionSuggestionResponse {

    private String merchantName;
    private Category suggestedCategory;
    private BigDecimal suggestedAmount;
    private BillingCycle suggestedBillingCycle;
    private PaymentMethod suggestedPaymentMethod;
    private int occurrenceCount;
    private String confidence;
    private LocalDate lastTransactionDate;
    private LocalDate nextPredictedRenewalDate;
    private List<Long> transactionIds;

    @JsonProperty("alreadySubscribed")
    private boolean alreadySubscribed;

    private String explanation;
}
