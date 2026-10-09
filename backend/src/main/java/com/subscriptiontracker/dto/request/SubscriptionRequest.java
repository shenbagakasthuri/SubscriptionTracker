package com.subscriptiontracker.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subscriptiontracker.model.enums.BillingCycle;
import com.subscriptiontracker.model.enums.Category;
import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import jakarta.validation.constraints.*;
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
public class SubscriptionRequest {

    @NotBlank(message = "Subscription name is required")
    @Size(max = 100, message = "Subscription name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Amount must be a valid monetary value with at most 8 integer digits and 2 decimals")
    private BigDecimal amount;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    @NotNull(message = "Next renewal date is required")
    private LocalDate nextRenewalDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @JsonProperty("isTrial")
    @Builder.Default
    private Boolean isTrial = false;

    private LocalDate trialEndDate;

    private SubscriptionStatus status;

    private LocalDate lastUsedDate;

    @Size(max = 500, message = "Cancel URL must not exceed 500 characters")
    private String cancelUrl;

    @AssertTrue(message = "Trial end date is required when isTrial is true")
    public boolean isTrialEndDateValid() {
        if (Boolean.TRUE.equals(isTrial)) {
            return trialEndDate != null;
        }
        return true;
    }
}
