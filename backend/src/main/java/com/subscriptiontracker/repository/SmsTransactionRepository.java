package com.subscriptiontracker.repository;

import com.subscriptiontracker.model.entity.SmsTransaction;
import com.subscriptiontracker.model.enums.TransactionDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsTransactionRepository extends JpaRepository<SmsTransaction, Long> {

    List<SmsTransaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    Optional<SmsTransaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDeduplicationFingerprint(Long userId, String deduplicationFingerprint);

    List<SmsTransaction> findByUserIdAndDirectionOrderByTransactionDateAsc(Long userId, TransactionDirection direction);
}
