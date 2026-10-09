package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.SubscriptionRequest;
import com.subscriptiontracker.dto.response.SubscriptionResponse;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse createSubscription(SubscriptionRequest request, String userEmail);

    List<SubscriptionResponse> getUserSubscriptions(String userEmail);

    SubscriptionResponse getSubscriptionById(Long id, String userEmail);

    SubscriptionResponse updateSubscription(Long id, SubscriptionRequest request, String userEmail);

    void deleteSubscription(Long id, String userEmail);
}
