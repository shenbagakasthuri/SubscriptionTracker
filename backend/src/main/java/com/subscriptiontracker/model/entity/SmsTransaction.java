package com.subscriptiontracker.model.entity;

import com.subscriptiontracker.model.enums.PaymentMethod;
import com.subscriptiontracker.model.enums.TransactionDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_fingerprint", columnNames = {"user_id", "deduplication_fingerprint"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String merchant;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "masked_account", length = 50)
    private String maskedAccount;

    @Column(name = "deduplication_fingerprint", nullable = false, length = 64)
    private String deduplicationFingerprint;

    @CreationTimestamp
    @Column(name = "imported_at", nullable = false, updatable = false)
    private LocalDateTime importedAt;
}
