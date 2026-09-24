package com.subscriptiontracker.repository;

import com.subscriptiontracker.model.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findBySubscriptionId(Long subscriptionId);
    boolean existsBySubscriptionIdAndAlertTypeAndSentAtAfter(Long subscriptionId, String alertType, LocalDateTime sentAt);
}
