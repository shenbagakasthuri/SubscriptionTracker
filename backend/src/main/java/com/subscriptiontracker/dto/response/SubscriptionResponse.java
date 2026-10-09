package com.subscriptiontracker.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subscriptiontracker.model.enums.BillingCycle;
import com.subscriptiontracker.model.enums.Category;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
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
public class SubscriptionResponse {

    private Long id;
    private Long userId;
    private String name;
    private Category category;
    private BigDecimal amount;
    private BillingCycle billingCycle;
    private LocalDate nextRenewalDate;
    private PaymentMethod paymentMethod;

    @JsonProperty("isTrial")
    private boolean isTrial;

    private LocalDate trialEndDate;
    private SubscriptionStatus status;
    private LocalDate lastUsedDate;
    private String cancelUrl;
    private LocalDateTime createdAt;
}
