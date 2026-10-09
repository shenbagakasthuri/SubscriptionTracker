package com.subscriptiontracker.mapper;

import com.subscriptiontracker.dto.request.SubscriptionRequest;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SubscriptionMapper {

    public Subscription toEntity(SubscriptionRequest request, User user) {
        if (request == null) {
            return null;
        }

        boolean isTrial = Boolean.TRUE.equals(request.getIsTrial());

        return Subscription.builder()
                .user(user)
                .name(request.getName().trim())
                .category(request.getCategory())
                .amount(request.getAmount())
                .billingCycle(request.getBillingCycle())
                .nextRenewalDate(request.getNextRenewalDate())
                .paymentMethod(request.getPaymentMethod())
                .isTrial(isTrial)
                .trialEndDate(isTrial ? request.getTrialEndDate() : null)
                .status(request.getStatus() != null ? request.getStatus() : SubscriptionStatus.ACTIVE)
                .lastUsedDate(request.getLastUsedDate())
                .cancelUrl(request.getCancelUrl())
                .build();
    }

    public void updateEntity(Subscription subscription, SubscriptionRequest request) {
        if (subscription == null || request == null) {
            return;
        }

        boolean isTrial = Boolean.TRUE.equals(request.getIsTrial());

        subscription.setName(request.getName().trim());
        subscription.setCategory(request.getCategory());
        subscription.setAmount(request.getAmount());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setNextRenewalDate(request.getNextRenewalDate());
        subscription.setPaymentMethod(request.getPaymentMethod());
        subscription.setTrial(isTrial);
        subscription.setTrialEndDate(isTrial ? request.getTrialEndDate() : null);
        if (request.getStatus() != null) {
            subscription.setStatus(request.getStatus());
        }
        subscription.setLastUsedDate(request.getLastUsedDate());
        subscription.setCancelUrl(request.getCancelUrl());
    }

    public SubscriptionResponse toResponse(Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser() != null ? subscription.getUser().getId() : null)
                .name(subscription.getName())
                .category(subscription.getCategory())
                .amount(subscription.getAmount())
                .billingCycle(subscription.getBillingCycle())
                .nextRenewalDate(subscription.getNextRenewalDate())
                .paymentMethod(subscription.getPaymentMethod())
                .isTrial(subscription.isTrial())
                .trialEndDate(subscription.getTrialEndDate())
                .status(subscription.getStatus())
                .lastUsedDate(subscription.getLastUsedDate())
                .cancelUrl(subscription.getCancelUrl())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    public List<SubscriptionResponse> toResponseList(List<Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return Collections.emptyList();
        }

        return subscriptions.stream()
                .map(this::toResponse)
                .toList();
    }
}
