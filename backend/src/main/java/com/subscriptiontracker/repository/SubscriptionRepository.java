package com.subscriptiontracker.repository;

import com.subscriptiontracker.model.entity.Subscription;
import com.subscriptiontracker.model.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserId(Long userId);
    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
    Optional<Subscription> findByIdAndUserId(Long id, Long userId);
    List<Subscription> findByStatusAndNextRenewalDate(SubscriptionStatus status, LocalDate renewalDate);
    List<Subscription> findByStatusAndIsTrialTrueAndTrialEndDate(SubscriptionStatus status, LocalDate trialEndDate);
}
