package com.itto.backend.domain.payment.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.common.entity.BaseTimeEntity;
import com.itto.backend.domain.common.enums.KnotTransactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "knot_transactions",
    indexes = {
        @Index(name = "idx_knot_transactions_user", columnList = "user_id"),
        @Index(name = "idx_knot_transactions_created", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class KnotTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private KnotTransactionType type;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "description", length = 200)
    private String description;
}
