package com.subscriptiontracker.repository;

import com.subscriptiontracker.model.entity.SubscriptionPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPriceHistoryRepository extends JpaRepository<SubscriptionPriceHistory, Long> {
    List<SubscriptionPriceHistory> findBySubscriptionIdOrderByChangedAtDesc(Long subscriptionId);
}
