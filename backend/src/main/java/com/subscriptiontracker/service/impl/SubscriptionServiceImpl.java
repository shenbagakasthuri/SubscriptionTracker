package com.subscriptiontracker.service.impl;

import com.subscriptiontracker.dto.request.SubscriptionRequest;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.mapper.SubscriptionMapper;
import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(SubscriptionRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        validateTrialDates(request);

        Subscription subscription = subscriptionMapper.toEntity(request, user);
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        return subscriptionMapper.toResponse(savedSubscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUserSubscriptions(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(user.getId());

        return subscriptionMapper.toResponseList(subscriptions);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        return subscriptionMapper.toResponse(subscription);
    }

    @Override
    @Transactional
    public SubscriptionResponse updateSubscription(Long id, SubscriptionRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        validateTrialDates(request);
        subscriptionMapper.updateEntity(subscription, request);
        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        return subscriptionMapper.toResponse(updatedSubscription);
    }

    @Override
    @Transactional
    public void deleteSubscription(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));

        subscriptionRepository.delete(subscription);
    }

    private User getUserByEmail(String userEmail) {
        String normalizedEmail = userEmail.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));
    }

    private void validateTrialDates(SubscriptionRequest request) {
        if (Boolean.TRUE.equals(request.getIsTrial()) && request.getTrialEndDate() == null) {
            throw new IllegalArgumentException("Trial end date is required when isTrial is true");
        }
    }
}
